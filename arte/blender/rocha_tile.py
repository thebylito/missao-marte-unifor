"""Periodic fractured Mars rock: tilted polygonal facets and restrained rust."""
import bpy
import numpy as np
from common import material

PERIOD = 255.0


def _rock_cells():
    """One stratified period, including a spatially balanced fracture network."""
    rng = np.random.default_rng(73195)
    gx, gz = np.meshgrid(np.arange(5), np.arange(5))
    sites = (np.stack((gx, gz), axis=-1).reshape(-1, 2) + .5
             + rng.uniform(-.30, .30, (25, 2)))*(PERIOD/5)
    # Stratified directions avoid a chance cluster of similarly lit facets.
    angles = rng.permutation(np.arange(25))*2*np.pi/25 + rng.uniform(-.1, .1, 25)
    tilts = rng.uniform(.10, .22, (25, 1))*np.stack(
        (np.cos(angles), np.sin(angles)), axis=-1)
    levels = rng.uniform(-2.5, 2.5, 25)
    levels -= levels.mean()
    brightness = rng.permutation(np.linspace(.96, 1.04, 25))
    hue = rng.permutation(np.linspace(-1, 1, 25))
    # sRGB swatches describe hue relationships, then become linear albedo.
    # Balance each row of cells so no broad patch of one pigment repeats.
    palette = np.array([[95, 45, 30], [105, 55, 32],
                        [95, 62, 38], [68, 40, 30]])/255
    palette = np.where(palette <= .04045, palette/12.92,
                       ((palette+.055)/1.055)**2.4)
    tint_rng = np.random.default_rng(90421)
    pigment = np.concatenate([tint_rng.permutation([0, 1, 2, 3, iz % 4])
                              for iz in range(5)])
    tints = palette[pigment]*brightness[:, None]
    tints *= 1 + hue[:, None]*np.array([.018, -.030, -.024])
    mean = tints.mean(axis=0); tints = mean + 0.30*(tints - mean)
    # Preserve the established linear-albedo budget under the fixed lights.
    tints *= np.array([.176, .0605, .0282])/tints.mean(axis=0)

    # Discover actual toroidal neighbours on a small, deterministic probe grid.
    # Suppress 30% of whole borders, spreading them among the cells instead of
    # cutting broad patches out of the network with low-frequency noise.
    q = (np.arange(100) + .5)*PERIOD/100
    px, pz = np.meshgrid(q, q)
    delta = np.remainder(np.stack((px, pz), axis=-1)[..., None, :]
                         - sites + PERIOD/2, PERIOD) - PERIOD/2
    ids = np.argsort(np.sum(delta*delta, axis=-1), axis=-1)[..., :2]
    edges = np.unique(np.sort(ids, axis=-1).reshape(-1, 2), axis=0)
    priority = rng.random(len(edges))
    incidence = np.zeros(25, dtype=int)
    weak = np.zeros(len(edges), dtype=bool)
    for _ in range(round(.30*len(edges))):
        score = incidence[edges].sum(axis=1) + priority
        score[weak] = np.inf
        chosen = np.argmin(score)
        weak[chosen] = True
        incidence[edges[chosen]] += 1
    strength = np.ones((25, 25))
    for (a, b), suppressed in zip(edges, weak):
        strength[a, b] = strength[b, a] = .035 if suppressed else 1.0
    return sites, tilts, levels, tints, strength


def _fields(x, z):
    """Evaluate periodic polygonal relief and linear RGB at arbitrary XZ points."""
    x, z = np.broadcast_arrays(np.asarray(x, dtype=np.float64),
                               np.asarray(z, dtype=np.float64))
    x, z = np.remainder(x, PERIOD), np.remainder(z, PERIOD)
    u, v = 2*np.pi*x/PERIOD, 2*np.pi*z/PERIOD
    rng = np.random.default_rng(49217)

    def noise_band(inner, outer, count, falloff=0):
        modes = [(kx, kz) for kx in range(outer+1)
                 for kz in range(-outer, outer+1)
                 if inner**2 <= kx*kx+kz*kz <= outer**2
                 and (kx > 0 or kz > 0)]
        field = np.zeros_like(x)
        energy = 0.0
        for index in rng.choice(len(modes), count, replace=False):
            kx, kz = modes[index]
            amplitude = np.hypot(kx, kz)**(-falloff)
            field += amplitude*np.sin(kx*u+kz*v+rng.uniform(0, 2*np.pi))
            energy += amplitude*amplitude
        return field/np.sqrt(energy*.5)

    grain = .425*np.tanh(noise_band(8, 40, 96, falloff=1))
    fracture_noise = np.tanh(noise_band(7, 14, 32))
    depth_noise = np.tanh(noise_band(9, 18, 32))
    # Less than one unit of warp per axis: almost straight Voronoi borders.
    wx = x + .55*np.sin(4*u+3*v+.4) + .25*np.sin(7*v+1.2)
    wz = z + .55*np.sin(3*u-4*v+1.7) + .25*np.sin(7*u+.9)
    sites, tilts, levels, tints, strength = _rock_cells()
    nearest = np.full_like(x, np.inf)
    second = np.full_like(x, np.inf)
    first_id = np.zeros(x.shape, dtype=int)
    second_id = np.zeros(x.shape, dtype=int)
    face = np.zeros_like(x)
    for index, ((fx, fz), (tx, tz), level) in enumerate(zip(sites, tilts, levels)):
        dx = np.remainder(wx-fx+PERIOD/2, PERIOD)-PERIOD/2
        dz = np.remainder(wz-fz+PERIOD/2, PERIOD)-PERIOD/2
        distance = np.hypot(dx, dz)
        closer = distance < nearest
        runner_up = distance < second
        second_id = np.where(closer, first_id,
                             np.where(runner_up, index, second_id))
        second = np.where(closer, nearest, np.minimum(second, distance))
        first_id = np.where(closer, index, first_id)
        nearest = np.minimum(nearest, distance)
        # No radial dome or broad blend: the nearest cell owns a tilted plane.
        face = np.where(closer, level+tx*dx+tz*dz, face)

    gap = second-nearest
    width = 3.5 + .30*fracture_noise
    border_strength = strength[first_id, second_id]
    # Resolved, soft V: both the floor and shoulder have smooth derivatives.
    groove = np.clip(1-gap/width, 0, 1)
    groove = groove*groove*(3-2*groove)
    crack = groove*border_strength
    depth = 3.25 + .65*depth_noise

    # Join tilted planes inside the groove instead of making a discontinuous
    # height jump that rasterizes as stitches. Include triple-junction peers;
    # the compact weights leave the angular facet interiors exactly planar.
    face_sum = face.copy()
    weight_sum = np.ones_like(x)
    for index, ((fx, fz), (tx, tz), level) in enumerate(zip(sites, tilts, levels)):
        dx = np.remainder(wx-fx+PERIOD/2, PERIOD)-PERIOD/2
        dz = np.remainder(wz-fz+PERIOD/2, PERIOD)-PERIOD/2
        distance = np.hypot(dx, dz)
        join_width = np.where(strength[first_id, index] < .1, 5.0, 3.5)
        weight = np.clip(1-(distance-nearest)/join_width, 0, 1)
        weight = weight*weight*(3-2*weight)
        weight *= first_id != index
        face_sum += weight*(level+tx*dx+tz*dz)
        weight_sum += weight
    face = face_sum/weight_sum

    strata = .10*np.sin(9*v+.4) + .06*np.sin(17*v+1.8)
    # No |k|=1-2 height or colour terms. Distributed small pits cannot form
    # a large cavity or a recognisable dark cluster within the repeated tile.
    pits = np.zeros_like(x)
    for iz in range(5):
        for ix in range(5):
            fx, fz = (np.array([ix, iz]) + rng.uniform(.15, .85, 2))*PERIOD/5
            radius = rng.uniform(1.5, 4.0)
            dx = np.remainder(x-fx+PERIOD/2, PERIOD)-PERIOD/2
            dz = np.remainder(z-fz+PERIOD/2, PERIOD)-PERIOD/2
            pits += rng.uniform(.55, 1.25)*np.maximum(
                1-(dx*dx+dz*dz)/(radius*radius), 0)**2
    bevel = np.clip(1-gap/6, 0, 1)
    bevel = .7*bevel*bevel*(3-2*bevel)*border_strength
    relief = face + grain + strata - depth*crack - bevel - pits
    # Small cavity darkening, with restrained upward-facet highlights. Colours
    # retain the previous mean albedo; the render is needed to confirm sRGB.
    cavity = np.clip(-face/10, 0, 1)
    upward = np.clip(-tilts[first_id, 1]/.22, 0, 1)
    speckle = np.tanh(noise_band(16, 60, 48, falloff=1))
    value = (1 + .025*speckle + .025*strata + .045*upward
             - .035*cavity - .10*pits)*(1-.45*crack)
    rgb = value[..., None]*tints[first_id]
    return relief, np.clip(rgb, .008, .32)


def build():
    scene = bpy.context.scene
    scene.cycles.samples = max(scene.cycles.samples, 64)
    scene.world.node_tree.nodes.get('Background').inputs['Strength'].default_value = .32
    # One-unit spacing throughout the visible period plus a 25-unit margin.
    # Equal sample phases at +/-127.5 preserve periodic interpolated normals;
    # the two-unit outer ring retains the full 3x3 span for cast shadows.
    margin = PERIOD/2 + 25
    coords = np.concatenate((np.arange(-1.5*PERIOD, -margin, 2),
                             np.arange(-margin, margin+1, 1),
                             np.arange(margin+2, 1.5*PERIOD+1, 2)))
    x, z = np.meshgrid(coords, coords)
    relief, rgb = _fields(x, z)
    verts = np.stack((x, -relief, z), axis=-1).reshape(-1, 3).astype(np.float32)
    n = len(coords)
    a = (np.arange(n-1)[:, None]*n + np.arange(n-1)[None, :]).ravel()
    faces = np.stack((a, a+1, a+n+1, a+n), axis=-1).astype(np.int32)
    mat = material('Dry oxidized cliff', (.175, .060, .028, 1), rough=.92)
    # Bulk RNA writes avoid hundreds of thousands of Python vertex lists.
    data = bpy.data.meshes.new('Periodic fractured rock mesh')
    data.vertices.add(len(verts))
    data.vertices.foreach_set('co', verts.ravel())
    data.loops.add(faces.size)
    data.loops.foreach_set('vertex_index', faces.ravel())
    data.polygons.add(len(faces))
    data.polygons.foreach_set('loop_start', np.arange(len(faces), dtype=np.int32)*4)
    data.polygons.foreach_set('loop_total', np.full(len(faces), 4, dtype=np.int32))
    data.update(calc_edges=True)
    obj = bpy.data.objects.new('Three by three periods of rock', data)
    bpy.context.collection.objects.link(obj)
    data.materials.append(mat)
    colors = obj.data.color_attributes.new(name='RockColour', type='FLOAT_COLOR', domain='POINT')
    rgba = np.ones((verts.shape[0], 4), dtype=np.float32)
    rgba[:, :3] = rgb.reshape(-1, 3)
    colors.data.foreach_set('color', rgba.ravel())
    attr = mat.node_tree.nodes.new('ShaderNodeAttribute')
    attr.attribute_name = 'RockColour'
    mat.node_tree.links.new(attr.outputs['Color'],
                            mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'])
    obj.data.polygons.foreach_set('use_smooth', np.ones(len(faces), dtype=bool))
    return lambda i: None


def postprocess(a):
    b = np.clip(np.asarray(a, dtype=np.float32), 0, 1).copy()
    b[:, :, 3] = 1
    # Only reconcile sampling noise at equivalent periodic texel centers.
    edge = (b[:, 0]+b[:, -1])*np.float32(.5)
    b[:, 0] = b[:, -1] = edge
    edge = (b[0]+b[-1])*np.float32(.5)
    b[0] = b[-1] = edge
    edge = (b[:, 0]+b[:, -1])*np.float32(.5)
    b[:, 0] = b[:, -1] = edge
    return b
