"""Original ceramic survey lander; pixels, nose +Z, camera on -Y.

No bpy at module scope: geometry_radius_bound() can run in plain Python.
The root is the collision/rotation origin; never recenter the gameplay ship.
"""
import math

NOZZLE_EXIT_Z = -28.0
NOZZLE_EXIT_RADIUS = 8.0
FOOT_X = 24.0
FOOT_BOTTOM_Z = -40.0

# z, X radius, Y radius. The command section terminates at Z=45.
HULL_PROFILE = (
    (-17, 13, 10), (-12, 19, 13), (3, 20, 14),
    (15, 17, 12), (28, 11, 8), (38, 6, 4.5), (45, .7, .7),
)


def _rings(profile, segments=32, caps=True):
    verts = [(rx * math.cos(2 * math.pi * k / segments),
              ry * math.sin(2 * math.pi * k / segments), z)
             for z, rx, ry in profile for k in range(segments)]
    faces = []
    for j in range(len(profile) - 1):
        for k in range(segments):
            a = j * segments + k
            b = j * segments + (k + 1) % segments
            faces.append((a, b, b + segments, a + segments))
    if caps:
        faces.extend((tuple(reversed(range(segments))),
                      tuple(range(len(verts) - segments, len(verts)))))
    return verts, faces


def _window(width, bottom, top, front_offset=0):
    """Sloping glazing plate with real thickness for the icon."""
    outline = [(-width * .78, bottom), (width * .78, bottom),
               (width, bottom + 3), (width * .62, top - 2),
               (0, top), (-width * .62, top - 2), (-width, bottom + 3)]
    front = [(x, -16 + (z - 12) * .36 + front_offset, z)
             for x, z in outline]
    verts = front + [(x, y + 1.8, z) for x, y, z in front]
    n = len(front)
    faces = [tuple(range(n)), tuple(reversed(range(n, 2 * n)))]
    faces += [(i, (i + 1) % n, (i + 1) % n + n, i + n)
              for i in range(n)]
    return verts, faces


def _parts():
    """One geometry specification for both construction and bound checks."""
    parts = []

    def add(kind, name, mat, *args, bevel=0):
        parts.append((kind, name, mat, args, bevel))

    add('mesh', 'Continuous ceramic command hull', 'ceramic',
        *_rings(HULL_PROFILE), bevel=.7)
    add('mesh', 'Graphite belly collar', 'graphite',
        *_rings(((-18.5, 12.5, 9.5), (-16, 15, 11), (-14.5, 15, 11))))
    add('mesh', 'Cockpit graphite gasket', 'graphite', *_window(10.5, 11, 31))
    add('mesh', 'Cyan observation window', 'cyan', *_window(8.8, 13, 29, -.6))
    # Cyan is confined to Z>=7.5, including the rear-facing band.
    add('mesh', 'Cyan shoulder band', 'cyan_band',
        *_rings(((7.5, 19.15, 13.45), (10, 18.53, 13.03)), caps=False))

    # Continuous hollow bell: outer wall -> annular exit lip -> inner wall.
    # No disk caps the mouth. Inner wall gets a separate dark material.
    add('mesh', 'Flared engine bell exterior', 'metal',
        *_rings(((NOZZLE_EXIT_Z, NOZZLE_EXIT_RADIUS, NOZZLE_EXIT_RADIUS),
                 (-24, 6.65, 6.65), (-19, 5, 5)), caps=False))
    add('mesh', 'Dark annular nozzle exit', 'refractory',
        *_rings(((NOZZLE_EXIT_Z, 6.7, 6.7),
                 (NOZZLE_EXIT_Z, NOZZLE_EXIT_RADIUS, NOZZLE_EXIT_RADIUS)), caps=False))
    lining_verts, lining_faces = _rings(((NOZZLE_EXIT_Z, 6.7, 6.7), (-20, 3.4, 3.4)), caps=False)
    add('mesh', 'Refractory bell interior', 'refractory',
        lining_verts, [tuple(reversed(face)) for face in lining_faces])
    add('mesh', 'Recessed dark engine throat', 'refractory',
        *_rings(((-20, 3.4, 3.4), (-19.8, 3.4, 3.4))))
    # Heat-darkened lip reads as a dark aperture edge in the exact side view;
    # the physically open interior becomes visible in the tilted icon.
    add('mesh', 'Heat-darkened nozzle lip', 'refractory',
        *_rings(((NOZZLE_EXIT_Z, NOZZLE_EXIT_RADIUS, NOZZLE_EXIT_RADIUS),
                 (-27.4, 7.7975, 7.7975)), caps=False))
    add('cube', 'Service hatch', 'metal', (0, -13.5, -7), (12, 2.5, 7), bevel=.7)
    add('cube', 'Hatch ceramic handle', 'ceramic', (0, -15, -7), (6, 1, 1.6), bevel=.3)
    for sign in (-1, 1):
        side = 'Port' if sign < 0 else 'Starboard'
        add('cube', side + ' ceramic service pod', 'ceramic',
            (sign * 21, 1, -2), (9, 17, 21), bevel=2)
        add('cube', side + ' pod inset', 'metal',
            (sign * 21, -7.7, -3), (4.8, 1.4, 11), bevel=.6)
        add('rod', side + ' landing strut', 'graphite',
            (sign * 15, -1, -10), (sign * FOOT_X, -1, -36.8), 2.6)
        add('rod', side + ' triangular brace', 'metal',
            (sign * 9, 2, -16), (sign * 24, 2, -35.5), 1.45)
        add('sphere', side + ' landing hinge', 'metal',
            (sign * 15.7, -2, -12), (3.5, 3, 3.5))
        add('cube', side + ' landing pad', 'graphite',
            (sign * FOOT_X, -1, FOOT_BOTTOM_Z + 2), (12, 11, 4), bevel=.5)
        add('cube', side + ' landing pad top plate', 'metal',
            (sign * FOOT_X, -1, -36.1), (10, 9, .8), bevel=.2)
        add('cube', side + ' amber marker', 'amber',
            (sign * 21, -8.6, -5), (2.4, .6, 2), bevel=.3)
    return parts


def parts_radius_bound(parts):
    """Conservative XZ bound, including a full bevel-width safety allowance.

    Bevels cut inward. Rod bounds use endpoint distance + radius; ellipsoids
    use an enclosing XZ rectangle. This bound holds at all gameplay rotations.
    """
    bounds = []
    for kind, name, _mat, args, bevel in parts:
        if kind == 'mesh':
            radius = max(math.hypot(x, z) for x, _y, z in args[0])
        elif kind in ('cube', 'sphere'):
            loc, size = args
            extent = tuple(v / 2 for v in size) if kind == 'cube' else size
            radius = math.hypot(abs(loc[0]) + extent[0], abs(loc[2]) + extent[2])
        else:
            a, b, width = args
            radius = max(math.hypot(a[0], a[2]), math.hypot(b[0], b[2])) + width
        bounds.append((radius + bevel, name))
    return max(bounds)


def geometry_radius_bound():
    return parts_radius_bound(_parts())


def ship_materials(overrides=None):
    from common import material, emissive

    finishes = {
        # Reflected key/rim light supplies volume; structure has no self-emission.
        'ceramic': ('Warm white ceramic', (.86, .85, .82, 1), .06, .35),
        'graphite': ('Neutral graphite', (.12, .12, .12, 1), .4, .4),
        'metal': ('Warm grey titanium', (.45, .43, .40, 1), .6, .3),
        'refractory': ('Dark engine interior', (.013, .011, .009, 1), 0, .9),
        # Opaque tinted glazing keeps the cyan mask solid at grazing angles.
        # Low roughness gives glass-like reflections; a small cyan fill
        # preserves saturation in shadow without replacing the key light.
        'cyan': ('Glossy cyan cockpit', (.02, .45, .62, 1), 0, .15, .30),
        'cyan_band': ('Glossy cyan band', (.02, .45, .62, 1), 0, .15, .25),
    }
    finishes.update(overrides or {})
    mats = {key: material(*finish) for key, finish in finishes.items()}
    mats['amber'] = emissive('Small amber markers', (1, .25, .015, 1), .85)
    return mats


def build_ship():
    from common import root

    bound, _ = geometry_radius_bound()
    assert bound < 56, 'Ship exceeds rotation-safe radius'
    ship = root('Original survey lander: collision center')
    return assemble(ship, _parts(), ship_materials(), bound)


def assemble(ship, parts, mats, bound):
    from common import mesh, cube, sphere, rod

    for kind, name, mat, args, bevel in parts:
        if kind == 'mesh':
            obj = mesh(name, *args, mats[mat], ship)
            if bevel:
                mod = obj.modifiers.new('Ceramic edge highlights', 'BEVEL')
                mod.width, mod.segments = bevel, 3
        elif kind == 'cube':
            cube(name, *args, mats[mat], ship, bevel=bevel)
        elif kind == 'sphere':
            sphere(name, *args, mats[mat], ship, subdiv=2)
        else:
            rod(name, *args, mats[mat], ship)
    ship['nozzle_exit_z'] = NOZZLE_EXIT_Z
    ship['foot_bottom_z'] = FOOT_BOTTOM_Z
    ship['geometry_radius_bound'] = bound
    return ship
