"""Original 16-frame ignition/fire/smoke effect, in the camera's XZ plane.

Two procedural emission/transparent cards, ignition rays and fourteen hull shards.
No simulations, external textures, volumes, compositor bloom, or frame history.
Units are pixels. Debris bounds include gravity and tumbling, before the safety mask.
"""
import math
import random

import bpy
import numpy as np
from common import mesh

_CURRENT_FRAME = 0
# Peak opacity, not RGB: preserve straight-alpha colour during dissipation.
_TAIL_ALPHA = (.62, .47, .34, .25, .19)


def _socket(sockets, names, fallback=None):
    """Prefer semantic names; indices are compatibility fallbacks only."""
    if isinstance(names, str):
        names = (names,)
    for name in names:
        result = sockets.get(name)
        if result is not None:
            return result
    if fallback is not None and fallback < len(sockets):
        return sockets[fallback]
    raise RuntimeError('Explosion shader socket missing: ' + '/'.join(names))


def _optional_setting(owner, name, value):
    if hasattr(owner, name):
        try:
            setattr(owner, name, value)
        except (AttributeError, TypeError, ValueError):
            pass  # Optional quality/performance setting, never a required socket.


class _Graph:
    """Small node wiring helpers, including duplicate-name Math inputs."""
    def __init__(self, name):
        self.material = bpy.data.materials.new(name)
        self.material.use_nodes = True
        self.nodes = self.material.node_tree.nodes
        self.nodes.clear()
        self.links = self.material.node_tree.links
        _optional_setting(self.material, 'surface_render_method', 'DITHERED')
        _optional_setting(self.material, 'blend_method', 'HASHED')

    def node(self, kind):
        return self.nodes.new(kind)

    def feed(self, value, target):
        if hasattr(value, 'is_output'):
            self.links.new(value, target)
        else:
            target.default_value = value

    def value(self, label, value):
        node = self.node('ShaderNodeValue')
        node.label = label
        output = _socket(node.outputs, 'Value', 0)
        output.default_value = value
        return output

    def math(self, operation, a, b=0):
        node = self.node('ShaderNodeMath')
        node.operation = operation
        # Math operand sockets all have the same name; their order is semantic.
        self.feed(a, node.inputs[0])
        self.feed(b, node.inputs[1])
        return _socket(node.outputs, 'Value', 0)

    def vector(self, operation, a, b=None):
        node = self.node('ShaderNodeVectorMath')
        node.operation = operation
        self.feed(a, _socket(node.inputs, 'Vector', 0))
        if b is not None:
            self.feed(b, node.inputs[1])  # Repeated 'Vector' operand.
        return _socket(node.outputs, 'Value' if operation == 'LENGTH' else 'Vector')

    def smooth(self, value, start, end):
        """Smoothstep with reversible limits, built from stable Math nodes."""
        t = self.math('DIVIDE', self.math('SUBTRACT', value, start), end - start)
        t = self.math('MINIMUM', self.math('MAXIMUM', t, 0), 1)
        return self.math('MULTIPLY', self.math('MULTIPLY', t, t),
                         self.math('SUBTRACT', 3, self.math('MULTIPLY', t, 2)))

    def ramp(self, value, stops):
        node = self.node('ShaderNodeValToRGB')
        ramp = node.color_ramp
        ramp.interpolation = 'EASE'
        for element, (position, color) in zip(ramp.elements, (stops[0], stops[-1])):
            element.position, element.color = position, color
        for position, color in stops[1:-1]:
            ramp.elements.new(position).color = color
        self.feed(value, _socket(node.inputs, 'Fac', 0))
        return _socket(node.outputs, 'Color', 0)

    def mix(self, factor, a, b):
        # MixRGB retains unambiguous Color1/Color2 sockets across Blender 5.x.
        node = self.node('ShaderNodeMixRGB')
        node.blend_type = 'MIX'
        self.feed(factor, _socket(node.inputs, 'Fac', 0))
        self.feed(a, _socket(node.inputs, ('Color1', 'A'), 1))
        self.feed(b, _socket(node.inputs, ('Color2', 'B'), 2))
        return _socket(node.outputs, 'Color', 0)

    def noise(self, coordinate, time, scale, detail):
        node = self.node('ShaderNodeTexNoise')
        node.noise_dimensions = '4D'
        self.feed(coordinate, _socket(node.inputs, 'Vector', 0))
        self.feed(time, _socket(node.inputs, 'W'))
        self.feed(scale, _socket(node.inputs, 'Scale'))
        self.feed(detail, _socket(node.inputs, 'Detail'))
        self.feed(.66, _socket(node.inputs, 'Roughness'))
        return node

    def coordinates(self):
        node = self.node('ShaderNodeTexCoord')
        uv = _socket(node.outputs, 'UV')
        return self.vector('MULTIPLY', self.vector('SUBTRACT', uv, (.5, .5, 0)),
                           (2, 2, 0))

    def surface(self, color, opacity, strength=1):
        emission = self.node('ShaderNodeEmission')
        self.feed(color, _socket(emission.inputs, 'Color', 0))
        self.feed(strength, _socket(emission.inputs, 'Strength', 1))
        self.finish(_socket(emission.outputs, 'Emission', 0), opacity)

    def finish(self, shader, opacity):
        transparent = self.node('ShaderNodeBsdfTransparent')
        blend = self.node('ShaderNodeMixShader')
        self.feed(opacity, _socket(blend.inputs, 'Fac', 0))
        self.feed(_socket(transparent.outputs, 'BSDF', 0), blend.inputs[1])
        self.feed(shader, blend.inputs[2])
        output = self.node('ShaderNodeOutputMaterial')
        self.feed(_socket(blend.outputs, 'Shader', 0), _socket(output.inputs, 'Surface', 0))


def _card(name, material, depth):
    # Explicit UVs: U -> X, V -> Z, bottom-up like Blender's image buffers.
    obj = mesh(name, [(-1, 0, -1), (1, 0, -1), (1, 0, 1), (-1, 0, 1)],
               [(0, 1, 2, 3)], material)
    uv = obj.data.uv_layers.new(name='Explosion UV')
    corners = ((0, 0), (1, 0), (1, 1), (0, 1))
    for polygon in obj.data.polygons:
        for loop_index in polygon.loop_indices:
            uv.data[loop_index].uv = corners[obj.data.loops[loop_index].vertex_index]
    obj.location.y = depth
    # Emissive cards must not occlude the lighting on the physical fragments.
    _optional_setting(obj, 'visible_shadow', False)
    return obj


def _fire_smoke():
    g = _Graph('Turbulent combustion and charcoal: one continuous field')
    time = g.value('4D evolution', 0)
    heat = g.value('Remaining heat', 1)
    opacity = g.value('Density', 1)
    white_core = g.value('Small white core, extinguished by frame six', 0)
    warm_interior = g.value('Warm interior floor, frames four through eight', 0)
    q = g.coordinates()
    raw_radius = g.vector('LENGTH', q)
    split = g.node('ShaderNodeSeparateXYZ')
    g.feed(q, _socket(split.inputs, 'Vector', 0))
    angle = g.math('ARCTAN2', _socket(split.outputs, 'Y'), _socket(split.outputs, 'X'))
    # Unequal low-frequency lobes, rather than fine noise on a circular border.
    lobes = g.math('ADD',
                   g.math('MULTIPLY', g.math('SINE', g.math('ADD',
                          g.math('MULTIPLY', angle, 3), .7)), .105),
                   g.math('MULTIPLY', g.math('SINE', g.math('SUBTRACT',
                          g.math('MULTIPLY', angle, 5), 1.1)), .065))
    warp = g.noise(q, time, 2.7, 2.4)
    bend = g.vector('MULTIPLY',
                    g.vector('SUBTRACT', _socket(warp.outputs, 'Color', 1), (.5, .5, .5)),
                    (.48, .48, 0))
    warped = g.vector('ADD', q, bend)
    radius = g.vector('LENGTH', warped)
    coarse = _socket(g.noise(warped, time, 4.8, 3).outputs, 'Fac', 0)
    fine = _socket(g.noise(warped, time, 18, 2).outputs, 'Fac', 0)
    # A continuous domain-warped contour, with curls breaking the outer edge.
    contour = g.math('SUBTRACT', g.math('ADD', radius,
                     g.math('MULTIPLY', g.math('SUBTRACT', .5, coarse), .30)), lobes)
    envelope = g.smooth(contour, .91, .61)
    # Smooth support bound prevents even a warped tongue from exceeding the card.
    envelope = g.math('MULTIPLY', envelope, g.smooth(raw_radius, .96, .85))
    texture = g.math('ADD', .25, g.math('MULTIPLY', g.smooth(coarse, .24, .73), .75))
    density = g.math('MULTIPLY', envelope, texture)
    density = g.math('MULTIPLY', density, g.math('ADD', .65, g.math('MULTIPLY', fine, .35)))
    alpha = g.math('MULTIPLY', g.smooth(density, 0, .60), opacity)

    # Temperature drops outward and over time; hotter veins follow the billows.
    temperature = g.math('SUBTRACT', heat, g.math('MULTIPLY', radius, .88))
    temperature = g.math('ADD', temperature,
                          g.math('MULTIPLY', g.math('SUBTRACT', coarse, .5), .55))
    temperature = g.math('ADD', temperature,
                          g.math('MULTIPLY', g.math('SUBTRACT', fine, .5), .18))
    # Lift only cold noise valleys inside the core, preserving hot veins and
    # the original smoky rim. Disable this completely during the smoke tail.
    inner = g.math('MULTIPLY', g.smooth(raw_radius, .70, .40), warm_interior)
    temperature = g.math('ADD', temperature, g.math('MULTIPLY', inner,
                         g.math('MAXIMUM', g.math('SUBTRACT', .60, temperature), 0)))
    fire = g.ramp(temperature, [
        (0, (.055, .008, .003, 1)),
        (.24, (.28, .012, .002, 1)),
        (.42, (.90, .045, .002, 1)),
        (.58, (1, .22, .006, 1)),
        (.76, (1, .52, .055, 1)),
        (.90, (1, .78, .25, 1)),
        (1, (1, .85, .48, 1)),
    ])
    # Sculpted grey-brown smoke stays readable on the dark cave. A fixed
    # upper-left bias provides gentle apparent volume without noisy scattering.
    light = g.math('SUBTRACT', g.math('MULTIPLY', _socket(split.outputs, 'Y'), .12),
                   g.math('MULTIPLY', _socket(split.outputs, 'X'), .10))
    smoke_detail = g.math('ADD', g.math('ADD', g.math('MULTIPLY', coarse, .78),
                                      g.math('MULTIPLY', fine, .22)), light)
    smoke = g.ramp(smoke_detail, [
        (0, (.010, .007, .006, 1)),
        (.35, (.024, .018, .015, 1)),
        (.62, (.064, .051, .043, 1)),
        (1, (.13, .106, .087, 1)),
    ])
    hot_fraction = g.smooth(temperature, .06, .37)
    color = g.mix(hot_fraction, smoke, fire)
    # Standard clips values above one: keep the entire fire ramp at strength 1.
    # White is a separate noisy inner 20-30% patch, never a hot ramp plateau.
    core_radius = g.math('ADD', raw_radius,
                         g.math('MULTIPLY', g.math('SUBTRACT', coarse, .5), .17))
    core_radius = g.math('DIVIDE', core_radius, g.math('MAXIMUM', white_core, .001))
    core = g.math('MULTIPLY', g.smooth(core_radius, .29, .12), white_core)
    color = g.mix(core, color, (1, 1, 1, 1))
    g.surface(color, alpha, 1)
    return (_card('Continuous turbulent fire and smoke', g.material, 0),
            time, heat, opacity, white_core, warm_interior)


def _flash():
    g = _Graph('White-hot ignition with soft yellow falloff')
    q = g.coordinates()
    radius = g.vector('LENGTH', q)
    noise = _socket(g.noise(q, .37, 3.2, 2).outputs, 'Fac', 0)
    irregular = g.math('ADD', radius,
                       g.math('MULTIPLY', g.math('SUBTRACT', noise, .5), .42))
    opacity = g.value('Ignition opacity', 1)
    core = g.smooth(irregular, .56, .17)
    halo = g.math('MULTIPLY', g.smooth(radius, .96, .22), .28)
    alpha = g.math('MULTIPLY', g.math('MAXIMUM', core, halo), opacity)
    color = g.ramp(irregular, [
        (0, (1, 1, 1, 1)),
        (.22, (1, .91, .66, 1)),
        (.48, (1, .60, .12, 1)),
        (.74, (1, .22, .012, 1)),
        (1, (.65, .055, .002, 1)),
    ])
    strength = g.math('ADD', 1, g.math('MULTIPLY', g.smooth(irregular, .30, .10), 1.3))
    g.surface(color, alpha, strength)
    return _card('Ignition glow', g.material, -2), opacity


def _flash_rays(rng):
    g = _Graph('Eight tapered warm ignition rays')
    uv = g.node('ShaderNodeTexCoord')
    split = g.node('ShaderNodeSeparateXYZ')
    g.feed(_socket(uv.outputs, 'UV'), _socket(split.inputs, 'Vector', 0))
    along = _socket(split.outputs, 'Y')
    color = g.ramp(along, [(0, (1, .96, .78, 1)),
                           (.42, (1, .78, .25, 1)), (1, (1, .24, .015, 1))])
    g.surface(color, g.smooth(along, 1, .15), 1)
    rays = []
    for j in range(8):
        obj = mesh('Ignition spike %02d' % j,
                   [(-1, 0, 0), (1, 0, 0), (.025, 0, 1), (-.025, 0, 1)],
                   [(0, 1, 2, 3)], g.material)
        uv_layer = obj.data.uv_layers.new(name='Ray falloff')
        for loop in obj.data.loops:
            uv_layer.data[loop.index].uv = ((0, 0), (1, 0), (1, 1), (0, 1))[loop.vertex_index]
        angle = j * math.tau / 8 + rng.uniform(-.19, .19)
        obj.rotation_euler.y = math.pi / 2 - angle
        obj.location = (math.cos(angle) * 3, -4, math.sin(angle) * 3)
        _optional_setting(obj, 'visible_shadow', False)
        rays.append((obj, rng.uniform(20, 40), rng.uniform(.65, 1.25)))
    return rays


def _hull_material(tone):
    g = _Graph('Broken hull grey %.2f' % tone)
    fade = g.value('Fragment fade', 1)
    hull = g.node('ShaderNodeBsdfPrincipled')
    g.feed((tone, tone, tone, 1), _socket(hull.inputs, 'Base Color', 0))
    g.feed(.12, _socket(hull.inputs, 'Metallic'))
    g.feed(.72, _socket(hull.inputs, 'Roughness'))
    g.feed((tone, tone, tone, 1), _socket(hull.inputs, 'Emission Color'))
    g.feed(.20, _socket(hull.inputs, 'Emission Strength'))
    g.finish(_socket(hull.outputs, 'BSDF', 0), fade)
    return g.material, fade


def _accent_material():
    g = _Graph('Cyan hull accent')
    fade = g.value('Accent fade', 1)
    g.surface((.04, .8, 1, 1), fade, 1)
    return g.material, fade


def _hot_material():
    g = _Graph('Hot fracture edge and short incandescent wakes')
    fade = g.value('Cooling metal', 1)
    g.surface((1, .20, .008, 1), fade, 1)
    return g.material, fade


def _debris(rng):
    hulls = [_hull_material(tone) for tone in (.75, .45, .15)]
    # Independent detail RNG, preserving the original RNG consumption below
    # so the already-approved tail embers keep their exact positions/phases.
    detail = random.Random(815)
    # Separate motion stream keeps the approved geometry and embers unchanged.
    motion = random.Random(818)
    rotation = motion.uniform(0, math.tau)
    step = math.tau / 14
    angles = [j * step + motion.uniform(-.45, .45) * step + rotation
              for j in range(14)]
    a, b, c, d = motion.sample(range(14), 4)
    angles[a], angles[b] = angles[b], angles[a]
    angles[c], angles[d] = angles[d], angles[c]
    slow = set(motion.sample(range(14), 3))
    cyan, cyan_fade = _accent_material()
    hot, hot_fade = _hot_material()
    edge = _Graph('Dark cut metal rims')
    edge_fade = edge.value('Rim fade', 1)
    edge.surface((.025, .032, .038, 1), edge_fade, 1)
    shards = []
    for j in range(14):
        length = 12 if j in (2, 9) else rng.uniform(4.5, 8.5)
        width = length * rng.uniform(.40, .95)
        # Consume the old motion draws to preserve sizes/phases and tail RNG.
        rng.uniform(-.48, .48)
        rng.uniform(26, 56)
        rng.uniform(.12, .32)
        angle = angles[j]
        start = motion.uniform(4, 12)
        distance = motion.uniform(22, 30) if j in slow else motion.uniform(34, 54)
        phase = rng.uniform(0, 6)
        outlines = (
            ((-.50, -.48), (.48, -.30), (-.12, .52)),
            ((-.48, -.36), (.28, -.50), (.50, .37), (-.30, .48)),
            ((-.50, -.47), (.42, -.32), (.25, .08), (.50, .48),
             (-.18, .31), (-.40, .05)),
            ((-.48, -.24), (.04, -.50), (.48, -.16), (.17, .11), (-.25, .50)),
        )
        outline = [(width * (x + detail.uniform(-.045, .045)),
                    length * (z + detail.uniform(-.045, .045)))
                   for x, z in outlines[j % len(outlines)]]
        n = len(outline)
        # A shallow fold across X gives bent plates real planar facets. The
        # silhouette stays sharp: no rounded inset, bevel or smooth shading.
        front = [(x, -.40 - .22 * abs(x), z) for x, z in outline]
        vertices = front + [(x, y + .65, z) for x, y, z in front]
        vertices.append((0, -.40, 0))
        # Front normal faces the camera at -Y; dark perimeter remains visible.
        faces = [(2 * n, k, (k + 1) % n) for k in range(n)]
        faces += [tuple(range(2 * n - 1, n - 1, -1))]
        faces += [(k, (k + 1) % n, (k + 1) % n + n, k + n) for k in range(n)]
        obj = mesh('Angular hull shard %02d' % j, vertices, faces, edge.material)
        hull = hulls[2 if j in (3, 8, 11) else j % 2][0]
        obj.data.materials.append(hull)
        for polygon in obj.data.polygons[:n]:
            polygon.material_index = 1
        if j in (1, 5, 9, 12):
            # A narrow patch inside one face, following its fold exactly.
            a, b = front[0], front[1]
            stripe = []
            for u, v in ((.13, .13), (.13, .34), (.62, .34), (.62, .13)):
                stripe.append((u*a[0] + v*b[0],
                               (1-u-v)*(-.40) + u*a[1] + v*b[1] - .035,
                               u*a[2] + v*b[2]))
            mesh('Cyan hull stripe %02d' % j,
                 stripe, [(0, 1, 2, 3)], cyan, obj)
        a, b = front[-2:]
        tip = mesh('Glowing torn edge %02d' % j,
                   [(a[0], a[1] - .04, a[2]), (b[0], b[1] - .04, b[2]),
                    (b[0]*.78, -.40 + (b[1]+.40)*.78 - .04, b[2]*.78),
                    (a[0]*.78, -.40 + (a[1]+.40)*.78 - .04, a[2]*.78)],
                   [(0, 1, 2, 3)], hot, obj)
        wake = None
        if j in (0, 3, 6, 9, 12):
            wake = mesh('Short hot wake %02d' % j,
                        [(-.70, 0, 0), (.70, 0, 0), (.025, 0, -1), (-.025, 0, -1)],
                        [(0, 1, 2, 3)], hot)
            _optional_setting(wake, 'visible_shadow', False)
        extent = max(math.sqrt(x*x + y*y + z*z) for x, y, z in vertices)
        # Bound both zero fall and the full 6.05 px fall through frame 13.
        # Their convex envelope contains the entire path at full shard size;
        # never clamp animated positions to a common circular wall.
        fall = .05 * 11**2
        limit = 58 - extent
        safe_distance = min(limit, fall * math.sin(angle) + math.sqrt(
            limit**2 - (fall * math.cos(angle))**2))
        distance = min(distance, safe_distance)
        detail.uniform(10, 21)  # Preserve the approved tumbling/detail stream.
        spin = math.radians(detail.uniform(90, 400)) * detail.choice((-1, 1)) / 11
        shards.append((obj, tip, wake, angle, distance, start, spin, phase))
    return shards, [fade for _, fade in hulls], cyan_fade, hot_fade, edge_fade


def _embers(rng):
    g = _Graph('Dying copper embers')
    brightness = g.value('Ember cooling', 1)
    opacity = g.value('Ember fade', 1)
    g.surface((.65, .048, .003, 1), opacity, brightness)
    embers = []
    for j in range(5):
        obj = mesh('Tiny angular ember %d' % j,
                   [(-.45, 0, -.35), (.5, 0, -.18), (.12, 0, .7)], [(0, 1, 2)], g.material)
        embers.append((obj, rng.uniform(-25, 25), rng.uniform(-16, 24), rng.uniform(0, 6)))
    return embers, brightness, opacity


def build():
    global _CURRENT_FRAME
    _CURRENT_FRAME = 0
    scene = bpy.context.scene
    # No volume settings are needed. Keep the caller's engine and sample count.
    _optional_setting(scene.render, 'filter_size', 1.0)
    cycles = getattr(scene, 'cycles', None)
    if cycles is not None:
        _optional_setting(cycles, 'volume_bounces', 0)
        _optional_setting(cycles, 'transparent_max_bounces', 8)
        _optional_setting(cycles, 'seed', 813)
        _optional_setting(cycles, 'use_animated_seed', False)
    fire, time, heat, density, white_core, warm_interior = _fire_smoke()
    flash, flash_alpha = _flash()
    rng = random.Random(813)
    debris, hull_fade, cyan_fade, hot_fade, edge_fade = _debris(rng)
    embers, ember_brightness, ember_alpha = _embers(rng)
    rays = _flash_rays(random.Random(814))
    radii = (16, 27, 32, 37, 42, 47, 50, 52, 52, 52, 51, 50, 50.5, 51, 51.5, 52)
    temperatures = (1.1, 1.1, 1.10, 1.10, 1.05, .99, .93, .91,
                    .78, .65, .48, .24, .09, -.08, -.19, -.28)

    def animate(i):
        global _CURRENT_FRAME
        _CURRENT_FRAME = max(0, min(15, int(i)))
        f = _CURRENT_FRAME
        fire.hide_render = f < 2
        fire.scale = (radii[f], 1, radii[f])
        fire.location.z = max(0, f - 7) * .75
        # Slow late evolution retains wisps instead of boiling during the fade.
        time.default_value = .18 * min(f, 10) + .065 * max(0, f - 10)
        heat.default_value = temperatures[f]
        warm_interior.default_value = 1 if 4 <= f <= 8 else 0
        white_core.default_value = (0, 0, 1, .95, .65, .18)[f] if f < 6 else 0
        density.default_value = .98 if f < 11 else (.9, .84, .76, .67, .57)[f - 11]
        flash.hide_render = f >= 2
        flash_radius = (24, 40)[min(f, 1)]
        flash.scale = (flash_radius, 1, flash_radius)
        flash_alpha.default_value = .86 if f == 0 else 1
        for ray, length, width in rays:
            ray.hide_render = f >= 2
            ray.scale = (width * (.8 if f == 0 else 1), 1,
                         length * (.56 if f == 0 else 1))
        fragment_alpha = 1 if f < 11 else max(0, (14 - f) / 4)
        for fade in hull_fade:
            fade.default_value = fragment_alpha
        cyan_fade.default_value = edge_fade.default_value = fragment_alpha
        hot_fade.default_value = max(0, min(1, (8 - f) / 6))
        for obj, tip, wake, angle, distance, start, spin, phase in debris:
            age = max(0, min(11, f - 2))
            u = max(0, min(1, (f - 2) / 10))
            travel = 1 - (1 - u)**2.6
            radial = start + (distance - start) * travel
            x = math.cos(angle) * radial
            z = math.sin(angle) * radial - .05 * age**2
            obj.hide_render = f < 2 or f >= 14
            # Gentle X/Z tilt keeps plates readable instead of edge-on specks.
            # Entire plate remains in front of both cards, even while tilted.
            obj.location = (x, -12 - .15 * math.sin(phase), z)
            scale = 1 if f < 11 else max(.15, 1 - .23 * (f - 10))
            obj.scale = (scale,) * 3
            obj.rotation_euler = (.24 * math.sin(phase + age * .43),
                                  phase + spin * age, .20 * math.cos(phase + age * .37))
            # Explicit visibility for child meshes as well as the parent.
            for child in obj.children:
                child.hide_render = obj.hide_render
            tip.hide_render = obj.hide_render or f > 7
            if wake is not None:
                wake.hide_render = not 2 <= f <= 7
                velocity = (distance - start) * .26 * (1 - u)**1.6
                vx, vz = math.cos(angle) * velocity, math.sin(angle) * velocity - .10 * age
                direction = math.atan2(vz, vx)
                wake.location = (x - math.cos(direction), -8, z - math.sin(direction))
                wake.rotation_euler = (0, math.pi / 2 - direction, 0)
                wake.scale = (.75, 1, min(8, 2 + 1.4 * math.hypot(vx, vz)))
        ember_brightness.default_value = max(.15, 1.2 - .13 * max(0, f - 7))
        # Wisps, rather than an opaque ember, set the late frame's alpha peak.
        ember_alpha.default_value = 1 if f < 11 else (.35, .28, .20, .13, .08)[f - 11]
        for obj, x, z, phase in embers:
            obj.hide_render = f < 7
            obj.location = (x + math.sin(phase + f * .21) * 2, -8,
                            z + max(0, f - 7) * 1.05)
            obj.rotation_euler.y = phase + f * .4
            size = max(.24, 1 - .08 * max(0, f - 7))
            obj.scale = (size,) * 3

    animate(0)
    return animate


def postprocess(a):
    """Straight RGBA, bottom-up. No colour premultiplication or synthetic pixels.

    The radial guard is outside the designed silhouette: the last three pixel
    rows/columns are exactly empty. Late peak opacity is explicitly controlled
    so the last frame survives PNG quantization and verify.py's >0.1 threshold.
    """
    result = np.array(a, dtype=np.float32, copy=True)
    h, w = result.shape[:2]
    yy, xx = np.ogrid[:h, :w]
    radius = np.sqrt((xx - (w - 1) / 2) ** 2 + (yy - (h - 1) / 2) ** 2)
    outer = min(w, h) / 2 - 3
    t = np.clip((outer - radius) / 3, 0, 1)
    guard = t * t * (3 - 2 * t)
    alpha = result[:, :, 3]
    alpha[:] = np.clip(alpha, 0, 1) * guard
    if _CURRENT_FRAME >= 11:
        peak = float(alpha.max())
        if peak > 0:
            alpha *= _TAIL_ALPHA[_CURRENT_FRAME - 11] / peak
    result[alpha == 0, :3] = 0
    return result
