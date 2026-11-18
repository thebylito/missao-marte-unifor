"""Original geometry helpers and shared orthographic scene, used only inside Blender."""
import math
import bpy
from mathutils import Vector

WHITE=(.72,.79,.82,1)
DARK=(.035,.055,.075,1)
METAL=(.19,.25,.29,1)
CYAN=(.015,.56,.78,1)
RUST=(.34,.105,.045,1)


def material(name, color, metallic=0, rough=.55, emission=0):
    m=bpy.data.materials.new(name)
    m.diffuse_color=color
    m.use_nodes=True
    p=m.node_tree.nodes.get('Principled BSDF')
    p.inputs['Base Color'].default_value=color
    p.inputs['Metallic'].default_value=metallic
    p.inputs['Roughness'].default_value=rough
    if emission:
        p.inputs['Emission Color'].default_value=color
        p.inputs['Emission Strength'].default_value=emission
    return m


def emissive(name,color,strength=1):
    m=bpy.data.materials.new(name)
    m.diffuse_color=color
    m.use_nodes=True
    n=m.node_tree.nodes
    n.clear()
    out=n.new('ShaderNodeOutputMaterial')
    e=n.new('ShaderNodeEmission')
    e.inputs['Color'].default_value=color
    e.inputs['Strength'].default_value=strength
    m.node_tree.links.new(e.outputs[0],out.inputs['Surface'])
    return m


def root(name):
    o=bpy.data.objects.new(name,None)
    bpy.context.collection.objects.link(o)
    return o


def finish(o,name,mat,parent=None):
    o.name=name
    if mat:
        o.data.materials.append(mat)
    if parent:
        o.parent=parent
    return o


def cube(name,loc,size,mat,parent=None,bevel=.6):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc)
    o=bpy.context.object
    o.scale=size
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    if bevel:
        b=o.modifiers.new('Soft machined edges','BEVEL')
        b.width=bevel
        b.segments=2
    return finish(o,name,mat,parent)


def sphere(name,loc,scale,mat,parent=None,subdiv=2):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=subdiv,radius=1,location=loc)
    o=bpy.context.object
    o.scale=scale
    return finish(o,name,mat,parent)


def cone(name,loc,r1,r2,depth,mat,parent=None,vertices=12):
    bpy.ops.mesh.primitive_cone_add(vertices=vertices,radius1=r1,radius2=r2,depth=depth,location=loc)
    return finish(bpy.context.object,name,mat,parent)


def rod(name,a,b,radius,mat,parent=None):
    a,b=Vector(a),Vector(b)
    o=cone(name,(a+b)/2,radius,radius,(b-a).length,mat,parent,vertices=8)
    o.rotation_euler=(b-a).to_track_quat('Z','Y').to_euler()
    return o


def mesh(name,verts,faces,mat,parent=None):
    m=bpy.data.meshes.new(name)
    m.from_pydata(verts,[],faces)
    m.update()
    o=bpy.data.objects.new(name,m)
    bpy.context.collection.objects.link(o)
    return finish(o,name,mat,parent)


def scene(w,h,opaque=False,engine='eevee',samples=16):
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for datablocks in (bpy.data.meshes,bpy.data.materials,bpy.data.cameras,bpy.data.lights):
        for block in list(datablocks):
            if block.users==0:
                datablocks.remove(block)
    s=bpy.context.scene
    if engine=='cycles':
        s.render.engine='CYCLES'
    else:
        for name in ('BLENDER_EEVEE','BLENDER_EEVEE_NEXT','CYCLES'):
            try:
                s.render.engine=name
                break
            except TypeError:
                continue
    if s.render.engine=='CYCLES':
        s.cycles.device='CPU'
        s.cycles.samples=samples
        s.cycles.use_denoising=False
        s.cycles.max_bounces=3
        s.cycles.diffuse_bounces=1
        s.cycles.glossy_bounces=1
        s.cycles.transparent_max_bounces=8
    if hasattr(s,'eevee'):
        if hasattr(s.eevee,'taa_render_samples'):
            s.eevee.taa_render_samples=samples
    s.render.resolution_x=w
    s.render.resolution_y=h
    s.render.resolution_percentage=100
    s.render.pixel_aspect_x=s.render.pixel_aspect_y=1
    s.render.film_transparent=not opaque
    s.render.image_settings.file_format='PNG'
    s.render.image_settings.color_mode='RGBA'
    s.render.image_settings.color_depth='8'
    s.render.image_settings.compression=30
    s.render.use_file_extension=True
    s.render.use_compositing=False
    s.render.use_sequencer=False
    s.view_settings.view_transform='Standard'
    s.view_settings.look='None'
    s.view_settings.exposure=0
    s.view_settings.gamma=1
    if not s.world:
        s.world=bpy.data.worlds.new('Ambient world')
    s.world.use_nodes=True
    bg=s.world.node_tree.nodes.get('Background')
    bg.inputs['Color'].default_value=(.22,.28,.34,1)
    bg.inputs['Strength'].default_value=.45
    bpy.ops.object.camera_add(location=(0,-2500,0))
    cam=bpy.context.object
    cam.name='Fixed orthographic XZ camera'
    cam.rotation_euler=(math.pi/2,0,0)
    cam.data.type='ORTHO'
    cam.data.sensor_fit='HORIZONTAL'
    cam.data.ortho_scale=w
    cam.data.clip_end=10000
    s.camera=cam
    for name,direction,energy,color in [
        ('Fixed upper left key',(-3,-4,5),2.2,(1,.88,.76)),
        ('Subtle cool rim',(4,2,3),.8,(.48,.73,1)),
    ]:
        bpy.ops.object.light_add(type='SUN')
        light=bpy.context.object
        light.name=name
        light.rotation_euler=(-Vector(direction)).to_track_quat('-Z','Y').to_euler()
        light.data.energy=energy
        light.data.color=color
        light.data.angle=math.radians(10)
    return s


def build_ship():
    r=root('Ship rotation pivot: cell center')
    shell=material('Porcelain ceramic hull',WHITE,.35,.32)
    dark=material('Graphite structure',DARK,.5,.36)
    metal=material('Titanium hardware',METAL,.7,.3)
    cyan=material('Cyan cockpit',CYAN,.45,.18,.3)
    glow=emissive('Navigation light',(.04,.8,1,1),1.5)
    # Nose + body together fit within radius 46 at all in-plane angles.
    cone('Tapered command hull',(0,0,3),19,11,39,shell,r)
    cone('Upward nose',(0,0,29),11,1,17,shell,r)
    cone('Belly ring',(0,0,-18),19.5,19.5,5,dark,r)
    cone('Main engine nozzle',(0,0,-25),10,6,10,metal,r)
    cone('Engine dark aperture',(0,0,-30.2),8,8,.6,dark,r)
    sphere('Cockpit dark gasket',(0,-13.9,13),(12,4.3,11),dark,r)
    sphere('Cyan forward glazing',(0,-16.1,14),(9.8,3.3,8.5),cyan,r)
    cube('Cockpit central bar',(0,-19.3,14),(1.4,1,16),shell,r,.3)
    for sign in (-1,1):
        cube('Shoulder pod',(sign*22,0,-2),(10,14,24),shell,r,3)
        cube('Pod graphite inset',(sign*22,-7.2,-3),(5,1.2,12),metal,r,.7)
        rod('Landing strut',(sign*15,0,-14),(sign*29,0,-33),2.1,metal,r)
        rod('Landing brace',(sign*20,2,-10),(sign*27,2,-31),1.2,shell,r)
        cube('Landing foot',(sign*29,-1,-35),(13,11,4),dark,r,.8)
        sphere('Navigation lamp',(sign*24,-7,-10),(2,1.2,2),glow,r,1)
    cube('Hull service hatch',(0,-18,-7),(12,2,7),metal,r,.8)
    cube('Hatch stripe',(0,-19.1,-7),(7,.6,1.6),shell,r,.2)
    return r


def build_platform(width,height,landing_y,color):
    top=height/2-landing_y
    r=root('Platform center pivot')
    deck=material('Brushed deck',(.31,.37,.40,1),.65,.38)
    dark=material('Undercarriage',DARK,.5,.55)
    trim=material('Rust orange edge',(.55,.19,.055,1),.25)
    lamp=emissive('Edge lamps',color,1.7)
    span=width-16
    cube('Flat landing deck',(0,0,top-4),(span,24,8),deck,r,0)
    cube('Lower frame',(0,2,top-12),(span-12,18,10),dark,r,1.5)
    cube('Front trim',(0,-12.3,top-6),(span,1.5,3),trim,r,.4)
    for x in range(-int(span/2)+18,int(span/2),22):
        cube('Deck seam',(x,-12.9,top-2.5),(1,.4,4.5),dark,r,0)
        rod('Underside diagonal brace',(x-7,-8,top-10),(x+6,-8,top-18),1,deck,r)
    for sign in (-1,1):
        x=sign*(span/2-5)
        cube('Lamp housing',(x,-3,top+1),(8,9,4),dark,r,.6)
        cube('Green or yellow edge beacon',(x,-7.7,top+1.7),(5,1.2,2.5),lamp,r,.4)
        cube('Foundation pad',(sign*span*.31,0,top-22),(20,18,5),deck,r,.8)
    # Visible front identification in original geometry: three short ivory bars.
    marking=material('Deck markings',(.78,.80,.7,1))
    for x in (-7,0,7):
        cube('Center guidance bar',(x,-13.2,top-5),(3,.5,2),marking,r,.2)
    return lambda i: None


def build_person(kind):
    r=root(kind+' center pivot, feet at bottom')
    colors={'astronauta':(.88,.23,.035,1),'engenheiro':(.035,.24,.65,1),'professor':(.13,.49,.22,1)}
    suit=material(kind+' suit',colors[kind],.05,.65)
    pale=material('Ivory patches',(.8,.82,.73,1))
    dark=material('Boots and seals',DARK,.15)
    skin=material('Warm face',(.62,.32,.16,1))
    glass=material('Helmet visor',(.025,.13,.20,1),.4,.2)
    # Bottom at Z=-32, exactly image row 63. These are the only boundary-touching parts.
    for sign in (-1,1):
        rod('Trouser leg',(sign*5,0,-10),(sign*5,0,-25),4,suit,r)
        cube('Boot',(sign*5,-1,-28),(8,11,8),dark,r,1)
    cube('Torso',(0,0,0),(18,11,24),suit,r,3)
    cube('Collar',(0,0,13),(13,10,3),pale,r,1)
    sphere('Head',(0,-.5,21),(8,6.5,8.5),skin,r)
    if kind=='astronauta':
        cube('Life support pack',(0,6,1),(16,9,21),pale,r,2)
        sphere('Pressure helmet',(0,-1,21),(10,8,10),pale,r)
        sphere('Dark panoramic visor',(0,-6,22),(8,3.8,6.8),glass,r)
        cube('Chest controls',(0,-6.1,2),(11,2,8),pale,r,1)
        for x in (-3,0,3):
            cube('Control key',(x,-7.3,2),(1.4,.5,2),dark,r,.2)
    elif kind=='engenheiro':
        hat=material('Safety yellow',(.95,.62,.035,1),.1)
        sphere('Hard hat crown',(0,0,26),(9,7,5),hat,r)
        cube('Hard hat brim',(0,-1,24),(21,16,2),hat,r,.7)
        cube('Utility belt',(0,-6,-7),(18,2.5,3),dark,r,.3)
        rod('Wrench handle',(-13,-4,-9),(-14,-4,5),1.4,pale,r)
        # Open jaw, modeled as a U rather than a downloaded tool mesh.
        rod('Wrench left jaw',(-16,-4,4),(-16,-4,9),1.3,pale,r)
        rod('Wrench right jaw',(-12,-4,4),(-12,-4,9),1.3,pale,r)
        rod('Wrench jaw bridge',(-16,-4,4),(-12,-4,4),1.3,pale,r)
    else:
        cube('Lab coat left lapel',(-4,-6.2,6),(3,1.2,12),pale,r,.4)
        cube('Lab coat right lapel',(4,-6.2,6),(3,1.2,12),pale,r,.4)
        cube('Clipboard',(-11,-8,-1),(10,2,16),material('Board',(.29,.15,.065,1)),r,.6)
        cube('Clipboard paper',(-11,-9.2,0),(8,.4,12),pale,r,.2)
        for z in (-3,0,3):
            cube('Notes',(-11,-9.5,z),(5,.2,.6),dark,r,0)
        for x in (-3.5,3.5):
            cube('Glasses lens',(x,-6.8,22),(6,1,3),glass,r,.7)
        rod('Glasses bridge',(-1,-7.4,22),(1,-7.4,22),.5,dark,r)
    rod('Resting upper arm',(-8,0,8),(-12,0,0),3.2,suit,r)
    rod('Resting forearm',(-12,0,0),(-11,-3,-6),2.7,suit,r)
    sphere('Resting glove',(-11,-3,-7),(3,3,3),pale,r)
    arm=root('Wave shoulder')
    arm.parent=r
    arm.location=(9,0,9)
    rod('Raised sleeve',(0,0,0),(5,0,8),3,suit,arm)
    rod('Raised forearm',(5,0,8),(4,0,16),2.5,suit,arm)
    sphere('Waving glove',(4,0,18),(3.2,3,4),pale,arm)
    def animate(i):
        # Small circular wrist/shoulder motion; all 8 loop frames distinct.
        t=2*math.pi*i/8
        arm.rotation_euler[1]=.22*math.sin(t)
        arm.rotation_euler[0]=.10*math.cos(t)
    return animate
