"""A deterministic displaced icosphere rotating about one tilted 3D axis."""
import math, random
from mathutils import Vector, Quaternion
from common import sphere, material, root, RUST

def build():
    rng=random.Random(613)
    r=root('Inclined asteroid spin')
    rock=material('Iron-rich basalt',RUST,0,.94)
    o=sphere('Sculpted asteroid',(0,0,0),(1,1,1),rock,r,3)
    for v in o.data.vertices:
        d=v.co.normalized()
        factor=1+.10*math.sin(d.x*8+d.z*3)+.08*math.sin(d.y*9-d.x*4)+rng.uniform(-.045,.045)
        v.co=(d.x*32*factor,d.y*26*factor,d.z*34*factor)
    # Bound every orientation to radius 44: at least four pixels of margin.
    # Uniform normalization preserves the existing faceted, elongated silhouette.
    max_radius=max(v.co.length for v in o.data.vertices)
    if max_radius>0:
        for v in o.data.vertices:
            v.co*=44.0/max_radius
    o.data.update()
    dark=material('Dark mineral',(.14,.058,.032,1),0,.95)
    pale=material('Ochre mineral',(.47,.22,.08,1),0,.88)
    o.data.materials.append(dark)
    o.data.materials.append(pale)
    for p in o.data.polygons:
        p.material_index=rng.choices([0,1,2],[.74,.14,.12])[0]
    axis=Vector((.36,.84,.40)).normalized()
    r.rotation_mode='QUATERNION'
    def animate(i):
        r.rotation_quaternion=Quaternion(axis,2*math.pi*i/32)
    return animate
