"""Chunky landing pads with exact contact planes and inset machinery."""
import bpy
from common import cube, emissive, material, mesh, rod, root, sphere


def _prism(name, outline, front, back, mat, parent):
    """Extrude a counterclockwise XZ outline; its front normal faces -Y."""
    n = len(outline)
    verts = [(x, y, z) for y in (front, back) for x, z in outline]
    faces = [tuple(range(n)), tuple(range(2*n-1, n-1, -1))]
    faces += [(i, i+n, (i+1) % n+n, (i+1) % n) for i in range(n)]
    return mesh(name, verts, faces, mat, parent)


def build_pad(width, height, landing_y, kind):
    """Build a solid pad from the landing row to the bottom cell edge."""
    if kind not in {'base', 'passageiro'}:
        raise ValueError('Unknown pad kind: ' + str(kind))
    bpy.context.scene.cycles.samples = 128
    bpy.context.scene.cycles.filter_width = 1.0
    home = kind == 'base'
    top = height/2 - landing_y
    bottom = -height/2
    half = width/2 - 3
    r = root(kind + ' landing pad')
    steel = material('Cool deck steel', (.30, .39, .46, 1), .35, .48)
    edge = material('Pale machined trim', (.48, .58, .63, 1), .3, .42)
    body = material('Graphite bulkhead', (.075, .105, .135, 1), .2, .72)
    panel = material('Blue grey service panels', (.16, .225, .28, 1), .25, .6)
    recess = material('Vent and gasket recesses', (.055, .080, .105, 1), .1, .85)
    amber = material('Ochre safety paint', (.66, .30, .055, 1), .05, .7)
    lamp = emissive('Green beacon' if home else 'Yellow beacon',
                    (.025, 1, .16, 1) if home else (1, .66, .025, 1), 4)
    beacon = emissive('Bright green end beacons', (.025, 1, .16, 1), 5.2) if home else lamp
    strip = emissive('Green deck identity' if home else 'Yellow deck identity',
                     (.025, 1, .16, 1) if home else (1, .66, .025, 1), 2)

    # No bevel or object above this plane in the central 70% of the deck.
    cube('Unbevelled contact slab', (0, 0, top-4),
         (2*half, 30, 8), steel, r, bevel=0)
    cube('Bright front lip', (0, -15.7, top-2.7),
         (2*half-2, 1.6, 2.4), edge, r, bevel=.35)
    cube('Recessed safety fascia', (0, -15.4, top-6),
         (2*half-4, 1, 3), recess, r, bevel=0)
    # Flush 1.6px identity line; the end hazard zones interrupt the run.
    hazard_inner = half-(40 if home else 30)
    for left, right in ((-half+3, -half+9), (-hazard_inner, hazard_inner),
                        (half-9, half-3)):
        # Leave clear breaks aligned with the orange chevron zones.
        cube('Deck identity strip', ((left+right)/2, -16.7, top-2.8),
             (right-left, .4, 1.6), strip, r, bevel=0)

    # Continuous opaque mass, including behind every panel and support rib.
    outline = [(-half+15, bottom), (half-15, bottom),
               (half-3, top-8), (-half+3, top-8)]
    _prism('Solid tapered machinery block', outline, -10, 12, body, r)
    foot = [(-half+15, bottom), (half-15, bottom),
            (half-12, bottom+7), (-half+12, bottom+7)]
    _prism('Continuous foundation plinth', foot, -13, 13, steel, r)
    cube('Foundation face inset', (0, -13.5, bottom+3),
         (width-48, 1, 3), panel, r, bevel=.3)

    panel_top = top-11
    panel_bottom = bottom+9
    panel_z = (panel_top+panel_bottom)/2
    panel_h = panel_top-panel_bottom
    for sign in (-1, 1):
        x = sign*width*.30
        pw = width*.19
        cube('Service panel gasket', (x, -10.8, panel_z),
             (pw+3, 2, panel_h+2), recess, r, bevel=1)
        cube('Service panel', (x, -12, panel_z),
             (pw, 1.5, panel_h), panel, r, bevel=.8)
        for dz in (-3, 1, 5) if home else (-2, 2):
            cube('Wide ventilation slot', (x, -13, panel_z+dz),
                 (pw-12, .6, 2), recess, r, bevel=.3)
        for sx in (-1, 1):
            sphere('Panel fastener', (x+sx*(pw/2-3), -13, panel_bottom+3),
                   (1.3, .7, 1.3), edge, r, subdiv=1)
        rib = [(sign*(half-17), bottom+6),
               (sign*(half-11), bottom+6),
               (sign*(half-3), top-9),
               (sign*(half-10), top-9)]
        if sign < 0:
            rib.reverse()
        _prism('Heavy outer support rib', rib, -13.4, -9, steel, r)

    # Broad isolated chevrons remain legible at native sprite size.
    for sign in (-1, 1):
        for offset in (0, 10, 20) if home else (0, 10):
            x = sign*(half-15-offset)
            quad = [(x-3, -16.1, top-7.4), (x+1, -16.1, top-7.4),
                    (x+4, -16.1, top-4.6), (x, -16.1, top-4.6)]
            mesh('Safety chevron', quad, [(0, 1, 2, 3)], amber, r)
    for fraction in (-.27, -.135, 0, .135, .27) if home else (-.20, 0, .20):
        cube('Inset deck signal', (fraction*width, -16.1, top-6),
             (4 if home else 3, .7, 1.8), lamp, r, bevel=.3)

    if home:
        # A horizontal tank and two flanged connections identify refuelling.
        tank_z = panel_z
        rod('Fuel reservoir', (-27, -13, tank_z), (27, -13, tank_z),
            7.5, steel, r)
        for x in (-19, 19):
            rod('Reservoir retaining collar', (x-1.6, -13, tank_z),
                (x+1.6, -13, tank_z), 8.2, body, r)
        for sign in (-1, 1):
            panel_inner = width*(.30-.19/2)
            rod('Horizontal fuel connection', (sign*26, -14, tank_z),
                (sign*(panel_inner+2), -14, tank_z), 1.8, amber, r)
            rod('Panel fuel flange', (sign*(panel_inner-1.5), -14, tank_z),
                (sign*(panel_inner+1.5), -14, tank_z), 3.3, edge, r)
        cube('Tank emblem backing', (0, -21, tank_z), (16, 1, 15), recess, r, .8)
        cube('Green fuel cross horizontal', (0, -21.7, tank_z),
             (12, .5, 3.5), strip, r, .2)
        cube('Green fuel cross vertical', (0, -21.7, tank_z),
             (3.5, .5, 12), strip, r, .2)
    else:
        cube('Pickup service hatch', (0, -11.5, panel_z),
             (28, 3, panel_h), steel, r, 1)
        cube('Hatch recessed handle', (0, -13.2, panel_z+1),
             (12, 1, 3), recess, r, .4)
        cube('Yellow identification bar', (0, -13.5, panel_z-4),
             (17, .7, 2), amber, r, .2)

    for sign in (-1, 1):
        x = sign*(half-10)
        post_h = 10 if home else 6
        cube('Beacon socket', (x, 1, top+1.2), (9, 10, 2.4), body, r, .4)
        cube('Short beacon post', (x, 1, top+post_h/2),
             (3.5, 5, post_h), steel, r, .4)
        cube('Beacon dark housing', (x, .5, top+post_h),
             (8, 7, 4), body, r, .7)
        cube('Emissive beacon lens', (x, -3.2, top+post_h),
             (7.15, 1.4, 3.38) if home else (5.5, 1.4, 2.6), beacon, r, .5)
    if home:
        x = -width*.40
        rod('Short telemetry mast', (x, 3, top), (x, 3, top+12), .7, steel, r)
        sphere('Telemetry tip', (x, 3, top+12), (1, 1, 1), lamp, r, subdiv=1)
    return lambda i: None
