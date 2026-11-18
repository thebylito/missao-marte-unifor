"""Three-quarter view of the same original ship, achieved by model rotation."""
import math
from nave_modelo import build_ship, NOZZLE_EXIT_Z, HULL_PROFILE, FOOT_BOTTOM_Z
from chama import build_flame, FLAME_LENGTHS

def build():
    import bpy

    ship=build_ship()
    # Exhaust inherits the same pose and participates in the evaluated bounds.
    flame, _animate_flame = build_flame(frame=0, parent=ship)
    flame.location.z = NOZZLE_EXIT_Z
    ship_height = HULL_PROFILE[-1][0] - FOOT_BOTTOM_Z
    flame.scale = (1, 1, .4 * ship_height / FLAME_LENGTHS[0])
    ship.rotation_euler=(math.radians(12),math.radians(-12),math.radians(38))
    bpy.context.view_layer.update()
    depsgraph=bpy.context.evaluated_depsgraph_get()
    # Fit actual rotated, beveled ship AND full flame envelope to 80% of the cell. The minimum
    # geometric margin is 25.6px, comfortably above the required 8px.
    points=[]
    for child in ship.children_recursive:
        if child.type != 'MESH':
            continue
        obj=child.evaluated_get(depsgraph)
        data=obj.to_mesh()
        try:
            points.extend(obj.matrix_world @ vertex.co for vertex in data.vertices)
        finally:
            obj.to_mesh_clear()
    xmin,xmax=min(p.x for p in points),max(p.x for p in points)
    zmin,zmax=min(p.z for p in points),max(p.z for p in points)
    scale=256*.8/max(xmax-xmin,zmax-zmin)
    ship.scale=(scale,)*3
    ship.location.x=-scale*(xmin+xmax)/2
    ship.location.z=-scale*(zmin+zmax)/2
    return lambda i: None
