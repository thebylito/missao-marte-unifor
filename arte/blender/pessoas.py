"""Original chunky passengers; coordinates are pixels in the camera's XZ plane.

The origin stays on the body axis. Boots end at Z=-32. Wave coordinates:
shoulder S=(7,0,6.5), elbow offset E=(4.8,0,10.5), hand offset H=(0,0,7.5).
For shoulder angle s and elbow angle e, the hand center is
    x = 7 + 4.8*cos(s) + 10.5*sin(s) + 7.5*sin(s+e)
    z = 6.5 - 4.8*sin(s) + 10.5*cos(s) + 7.5*cos(s+e),
where s=.12*cos(t), e=.75*sin(t), t=2*pi*i/8.
Across the eight frames its center spans X=6.565..17.001, Z=22.488..24.945.
The palm radii are (3.8,2.6,3.4); the outer thumb at (3.3,0,6.7)
has radii (1.25,1.7,1.65), both in the elbow's local coordinates.
Their combined smooth envelope spans X=2.892..20.673, Z=18.175..28.352.
Before outlining, approximate minimum hand-to-head silhouette gaps are
4.94 px (helmet), 2.19 px (hard hat), and 5.12 px (hair), all at frame 6.
These use enclosing smooth ellipses; the ico meshes lie inside them.
All child geometry uses local coordinates; common's helpers assign parents
without preserving world transforms, which is exactly what these pivots need.
"""
import math

import numpy as np

from common import material, emissive, root, cube, sphere, cone, rod, mesh


def _prism(name, outline, front, back, mat, parent):
    """Extrude a counterclockwise XZ polygon toward +Y (front faces -Y)."""
    n = len(outline)
    verts = [(x, y, z) for y in (front, back) for x, z in outline]
    faces = [tuple(range(n)), tuple(range(2 * n - 1, n - 1, -1))]
    faces += [(i, i + n, (i + 1) % n + n, (i + 1) % n)
              for i in range(n)]
    return mesh(name, verts, faces, mat, parent)


def _palette(kind):
    color = {'astronauta': (1.0, .38, .04, 1),
             'engenheiro': (.04, .22, .72, 1),
             'professor': (.10, .50, .20, 1)}[kind]
    return {
        'suit': material(kind + ' fabric', color, rough=.5),
        'white': material('Warm white trim', (.92, .94, .87, 1), rough=.6),
        'dark': material('Ink and boot rubber', (.018, .025, .035, 1), rough=.7),
        'skin': material('Warm light skin', (.88, .60, .38, 1), rough=.8),
        'yellow': material('Safety yellow', (1.0, .76, .025, 1), rough=.55),
        'metal': material('Light silver tools', (.67, .76, .83, 1),
                          metallic=.45, rough=.32),
        'brown': material('Leather and clipboard', (.30, .12, .045, 1)),
    }


def _legs(r, p, astronaut=False):
    for sign in (-1, 1):
        x = sign * 4.8
        cube('Trouser leg', (x, 0, -22),
             (7.6 if astronaut else 6.2, 8, 12), p['suit'], r, .8)
        # No bevel: the entire lower face is exactly on the image bottom.
        cube('Flat sole boot', (x, -1.3, -29),
             (8.2 if astronaut else 7.4, 10, 6),
             p['white'] if astronaut else p['dark'], r, 0)
        if astronaut:
            cube('White knee band', (x, -.1, -21), (7.8, 8.3, 2.2),
                 p['white'], r, .25)


def _face(r, p, professor=False):
    z = 5 if professor else 3.8
    sphere('Face looking toward camera', (0, -.5, z), (7.4, 5.5, 9),
           p['skin'], r, subdiv=3)
    for sign in (-1, 1):
        sphere('Small ear', (sign * 7, -.2, z), (1.2, 2, 2), p['skin'], r)
        # Professor's pupils are in front of the pale opaque lens insets.
        sphere('Visible dark eye', (sign * (3.4 if professor else 2.9),
                                   -7.15 if professor else -5.8,
                                   6.5 if professor else 5),
               (.85, .55, 1.0), p['dark'], r)
    cube('Small smile', (0, -5.8, z - 4), (2.8, .5, .55), p['dark'], r, .15)


def _astronaut(r, p):
    _legs(r, p, astronaut=True)
    cube('Life support backpack', (0, 5.5, -7), (23, 8, 18), p['white'], r, 1.4)
    for sign in (-1, 1):
        cube('Backpack side rail', (sign * 10.7, 3, -7), (2.4, 5, 13),
             p['dark'], r, .5)
    cube('Broad pressure suit', (0, 0, -8), (20, 11, 20), p['suit'], r, 2)
    cube('White waist belt', (0, -.1, -16), (20.2, 11.3, 2.5), p['white'], r, .5)
    sphere('Large white pressure helmet', (0, -1, 5), (10.5, 7.5, 10),
           p['white'], r, subdiv=3)
    visor = material('Deep blue glass visor', (.012, .045, .075, 1),
                     metallic=.35, rough=.16)
    sphere('Panoramic dark visor', (0, -7.8, 5.4), (8.2, 2.6, 6.8), visor, r, subdiv=3)
    glint = emissive('Cyan white visor reflection', (.55, .94, 1, 1), .9)
    sphere('Upper left visor glint', (-3.7, -10.1, 8.6), (1.6, .65, 1.1), glint, r)
    rod('Short helmet antenna', (-6.8, .5, 11.5), (-7.8, .5, 20.5),
        .65, p['metal'], r)
    sphere('Antenna bulb', (-7.8, .5, 21), (1.3, 1.3, 1.3), p['yellow'], r)
    cube('Chest control box', (0, -6.1, -9.5), (10, 2, 7), p['white'], r, .7)
    cube('Chest display', (-1.8, -7.2, -8.7), (4.2, .4, 2.5), visor, r, .2)
    for z in (-8.3, -10.8):
        cube('Chest control key', (2.8, -7.3, z), (1.5, .5, 1.4), p['dark'], r, .15)
    rod('Resting sleeve', (-9, 0, -1), (-12, 0, -8), 3, p['suit'], r)
    rod('Resting forearm', (-12, 0, -8), (-12, -2, -14), 2.6, p['suit'], r)
    sphere('Resting white glove', (-12, -2, -15), (3, 3, 3), p['white'], r)
    cube('Sample canister', (-13, -2, -21), (5.5, 5, 8), p['white'], r, .8)
    rod('Canister handle', (-13, -2, -18), (-12, -2, -15), .75, p['metal'], r)
    cube('Canister orange label', (-13, -4.6, -21), (3.5, .4, 3), p['suit'], r, .2)


def _hard_hat(r, p):
    """17x7 upper hemisphere on a flat 21x12.5 brim; rounded yellow ridge.

    Coordinates below are local to the (-.5,0,.25) hat pivot.
    World dome equator: (-.5,-.5,11.25), radii (8.5,6,7), top Z=18.25.
    World brim center: (-.5,-1.25,10.5), Z=9.75..11.25; front Y=-7.5.
    World ridge envelope: X=-.95..-.05, Y=-7.05..6.05, top Z=18.8.
    Raising this assembly 1.5 px would violate the unchanged wave clearance.
    """
    hat = root('Engineer hard hat clearance offset')
    hat.parent = r
    hat.location = (-.5, 0, .25)
    r = hat
    segments, rings = 48, 12
    verts = []
    for j in range(rings):
        latitude = math.pi * j / (2 * rings)
        for i in range(segments):
            longitude = math.tau * i / segments
            verts.append((8.5 * math.cos(latitude) * math.cos(longitude),
                          -.5 + 6 * math.cos(latitude) * math.sin(longitude),
                          11 + 7 * math.sin(latitude)))
    apex = len(verts)
    verts.append((0, -.5, 18))
    faces = []
    for j in range(rings - 1):
        for i in range(segments):
            k = (i + 1) % segments
            faces.append((j * segments + i, j * segments + k,
                          (j + 1) * segments + k, (j + 1) * segments + i))
    for i in range(segments):
        faces.append(((rings - 1) * segments + i,
                      (rings - 1) * segments + (i + 1) % segments, apex))
    faces.append(tuple(reversed(range(segments))))
    dome = mesh('Smooth hard hat dome', verts, faces, p['yellow'], r)
    for face in dome.data.polygons:
        face.use_smooth = len(face.vertices) != segments

    brim = cone('Flat elliptical hard hat brim', (0, -1.25, 10.25),
                10.5, 10.5, 1.5, p['yellow'], r, vertices=64)
    brim.scale.y = 6.25 / 10.5
    for face in brim.data.polygons:
        face.use_smooth = len(face.vertices) == 4

    # A continuous round tube follows the crown from front over top to back.
    # Its lower half intersects the dome, leaving a small raised center rib.
    steps, sides = 32, 8
    verts, faces = [], []
    for j in range(steps + 1):
        t = math.pi * j / steps
        y, z = -.5 - 6.1 * math.cos(t), 11 + 7.1 * math.sin(t)
        length = math.hypot(7.1 * math.cos(t), 6.1 * math.sin(t))
        ny, nz = -7.1 * math.cos(t) / length, 6.1 * math.sin(t) / length
        for i in range(sides):
            a = math.tau * i / sides
            verts.append((.45 * math.cos(a), y + .45 * math.sin(a) * ny,
                          z + .45 * math.sin(a) * nz))
    for j in range(steps):
        for i in range(sides):
            k = (i + 1) % sides
            faces.append((j * sides + i, (j + 1) * sides + i,
                          (j + 1) * sides + k, j * sides + k))
    faces += [tuple(range(sides)),
              tuple(steps * sides + i for i in reversed(range(sides)))]
    ridge = mesh('Rounded yellow hard hat center ridge', verts, faces, p['yellow'], r)
    for face in ridge.data.polygons:
        face.use_smooth = len(face.vertices) == 4


def _engineer_face(r, p):
    """Broad front skin plane keeps both eyes readable below the shallow brim.

    Head bounds: X=+/-7.4, Y=-6..4, Z=0..12, with 2 px rounded edges.
    The central skin plane is Y=-6; its lower bevel remains in front of
    the coveralls down to about Z=.414, exposing over 9 px below the brim.
    A little warm fill prevents the shaded cheeks from turning gray while
    retaining the shared upper-left key light. Other passengers use _face.
    """
    skin = material('Engineer softly filled skin', (.88, .60, .38, 1),
                    rough=.8, emission=.15)
    cube('Engineer broad rounded head', (0, -1, 6), (14.8, 10, 12),
         skin, r, 2)
    for sign in (-1, 1):
        sphere('Engineer small skin ear', (sign * 7, -.2, 5),
               (1.2, 2, 2), skin, r)
    eyes = material('Engineer near black eyes', (.02, .02, .03, 1), rough=.8)
    for x in (-2, 2):
        # Full 1.6x2 px silhouettes, front Y=-6.85 (0.85 px proud).
        cube('Engineer visible eye', (x, -6.5, 7.25), (1.6, .7, 2),
             eyes, r, .12)
    mouth = material('Engineer dark brown smile', (.12, .045, .02, 1), rough=.8)
    cube('Engineer small mouth', (0, -6.35, 4.25), (3, .5, .8),
         mouth, r, .1)


def _engineer(r, p):
    _legs(r, p)
    cube('Blue coveralls', (0, 0, -8), (18, 10, 20), p['suit'], r, 1.4)
    cube('Hi vis chest stripe', (0, -5.2, -7), (18, .7, 2.8), p['yellow'], r, .2)
    cube('Brown tool belt', (0, -.2, -15), (18.4, 10.5, 3), p['brown'], r, .4)
    cube('Belt buckle', (0, -5.7, -15), (3.4, .6, 2.3), p['metal'], r, .25)
    for x in (-6, 6):
        cube('Belt pouch', (x, -5.8, -16), (3.5, 2, 4), p['brown'], r, .3)
    _engineer_face(r, p)
    _hard_hat(r, p)
    rod('Resting blue sleeve', (-8, 0, -1), (-11.5, 0, -8), 2.7, p['suit'], r)
    rod('Tool arm', (-11.5, 0, -8), (-14.5, -2, -15), 2.3, p['suit'], r)
    rod('Yellow resting cuff', (-13.8, -1.5, -13.4), (-14.5, -2, -15),
        2.45, p['yellow'], r)
    sphere('Tool holding glove', (-14.5, -2.8, -16), (2.6, 2.8, 2.6), p['white'], r)
    # 17 px wrench, at least 3 px clear of the body: open air between the jaws.
    cube('Wrench shaft', (-16, -5.7, -19), (2.1, 1.8, 12), p['metal'], r, .35)
    cube('Wrench jaw base', (-16, -5.7, -13), (7.2, 1.8, 2), p['metal'], r, .25)
    for x in (-18.7, -13.3):
        cube('Open wrench jaw', (x, -5.7, -10.5), (1.8, 1.8, 5), p['metal'], r, .2)


def _professor(r, p):
    trousers = dict(p, suit=p['dark'])
    _legs(r, trousers)
    cube('Green coat torso', (0, 0, -7), (17, 10, 18), p['suit'], r, 1.2)
    _prism('Flared coat skirt', [(-11.5, -23), (11.5, -23),
                                 (7.5, -10), (-7.5, -10)],
           -5.1, 5.1, p['suit'], r)
    # A solid triangle makes a V opening, without the old inverted-U lapels.
    _prism('White shirt in V opening', [(0, -10), (5.5, 1.2), (-5.5, 1.2)],
           -5.8, -5.4, p['white'], r)
    red = material('Small red tie', (.72, .035, .025, 1), rough=.6)
    _prism('Red tie', [(0, -7.8), (1.3, -5.5), (.65, -.4),
                       (-.65, -.4), (-1.3, -5.5)], -6.2, -5.9, red, r)
    cube('Tie knot', (0, -6.1, -.1), (1.8, .6, 1.6), red, r, .2)
    for z in (-12, -15, -18):
        sphere('Coat button', (1.5, -5.4, z), (.55, .45, .55), p['dark'], r, subdiv=1)
    _face(r, p, professor=True)
    for sign in (-1, 1):
        sphere('White side hair tuft', (sign * 7.2, -.1, 8.2), (2.3, 4, 4.5), p['white'], r)
    sphere('Swept white hair', (-1.2, 0, 12.6), (6.7, 4.5, 2.3), p['white'], r)
    lenses = material('Pale blue white lenses', (.70, .87, .94, 1),
                      rough=.35, emission=.15)
    for x in (-3.4, 3.4):
        cube('Thin dark glasses frame', (x, -6.1, 6.5), (6.1, .8, 4.3), p['dark'], r, .45)
        cube('Light glasses lens', (x, -6.65, 6.5), (4.9, .4, 3.1), lenses, r, .3)
    rod('Glasses bridge', (-.7, -6.7, 6.5), (.7, -6.7, 6.5), .4, p['dark'], r)
    for x in (-1.2, 1.2):
        sphere('Small white moustache', (x, -6, .7), (1.7, .8, .7), p['white'], r)
    rod('Resting coat sleeve', (-7.5, 0, -1), (-11, -1, -7), 2.8, p['suit'], r)
    rod('Clipboard forearm', (-11, -1, -7), (-13, -4, -11), 2.5, p['suit'], r)
    cube('Clipboard board', (-11, -8, -15), (10, 1.7, 14), p['brown'], r, .45)
    cube('Clipboard paper', (-11, -8.95, -15.5), (8, .3, 11), p['white'], r, .1)
    for z, width in ((-12.5, 5.7), (-15, 5.7), (-17.5, 3.8)):
        cube('Written note', (-11.3, -9.2, z), (width, .25, .65), p['dark'], r, 0)
    cube('Clipboard metal clip', (-11, -9.2, -8.8), (3.8, .7, 2.2), p['metal'], r, .25)
    sphere('Clipboard holding hand', (-14.3, -7.4, -10.7), (2.5, 2.4, 2.5), p['skin'], r)


def _wave(r, p, kind):
    shoulder = root('Waving shoulder pivot')
    shoulder.parent = r
    shoulder.location = (7, 0, 6.5)
    rod('Raised upper sleeve', (0, 0, 0), (4.8, 0, 10.5), 2.6, p['suit'], shoulder)
    sphere('Rounded shoulder', (0, 0, 0), (2.8, 2.8, 2.8), p['suit'], shoulder)
    elbow = root('Waving elbow pivot')
    elbow.parent = shoulder
    elbow.location = (4.8, 0, 10.5)
    sphere('Rounded elbow', (0, 0, 0), (2.4, 2.4, 2.4), p['suit'], elbow)
    rod('Upright forearm', (0, 0, 0), (0, 0, 5), 2.2, p['suit'], elbow)
    cuff = p['yellow'] if kind == 'engenheiro' else p['white']
    rod('Contrasting waving cuff', (0, 0, 4), (0, 0, 5.5), 2.4, cuff, elbow)
    hand = p['skin'] if kind == 'professor' else p['white']
    sphere('Open waving mitten palm', (0, 0, 7.5), (3.8, 2.6, 3.4),
           hand, elbow, subdiv=3)
    sphere('Outer waving thumb', (3.3, 0, 6.7), (1.25, 1.7, 1.65),
           hand, elbow, subdiv=3)

    def animate(i):
        t = 2 * math.pi * (i % 8) / 8
        shoulder.rotation_euler[1] = .12 * math.cos(t)
        elbow.rotation_euler[1] = .75 * math.sin(t)

    animate(0)
    return animate


def build_person(kind):
    builders = {'astronauta': _astronaut, 'engenheiro': _engineer,
                'professor': _professor}
    builder = builders[kind]
    r = root(kind + ' centered body; boot soles at Z=-32')
    p = _palette(kind)
    builder(r, p)
    return _wave(r, p, kind)


def _smooth_opaque_rgb(a):
    """Two edge-preserving 3x3 means; leave alpha and nonopaque RGB intact."""
    opaque = a[:, :, 3] >= .99
    padded_opaque = np.pad(opaque, 1, mode='constant', constant_values=False)
    h, w = opaque.shape
    for _ in range(2):
        # Each pass reads a snapshot, so traversal order cannot smear details.
        rgb = a[:, :, :3].copy()
        padded_rgb = np.pad(rgb, ((1, 1), (1, 1), (0, 0)), mode='constant')
        total = rgb.copy()
        count = np.ones((h, w), dtype=np.float32)
        for dy in range(3):
            for dx in range(3):
                if (dy, dx) == (1, 1):
                    continue
                other = padded_rgb[dy:dy + h, dx:dx + w]
                similar = (opaque & padded_opaque[dy:dy + h, dx:dx + w]
                           & (np.max(np.abs(other - rgb), axis=2) < .12))
                total += np.where(similar[:, :, None], other, 0)
                count += similar
        a[opaque, :3] = (total / count[:, :, None])[opaque]
    return a


def postprocess(a):
    """Smooth opaque RGB, then one-pixel outline; straight RGBA, bottom first."""
    a = _smooth_opaque_rgb(a)
    solid = np.pad(a[:, :, 3] >= .5, 1, mode='constant', constant_values=False)
    neighbor = (solid[:-2, 1:-1] | solid[2:, 1:-1]
                | solid[1:-1, :-2] | solid[1:-1, 2:])
    edge = (a[:, :, 3] < .5) & neighbor
    a[edge, :3] = (.07, .045, .035)
    a[edge, 3] = np.maximum(a[edge, 3], .85)
    # Never wrap an outline around the bottom or into a cell corner.
    a[0, 0] = a[0, -1] = a[-1, 0] = a[-1, -1] = 0
    return a
