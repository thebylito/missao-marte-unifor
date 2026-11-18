"""Dusty emissive sky and five shaded, haze-softened distant Mars ranges."""
import numpy as np
from common import cube, emissive, mesh


def _linear(rgb):
    rgb = np.asarray(rgb, dtype=np.float64)
    return np.where(rgb <= .04045, rgb/12.92, ((rgb+.055)/1.055)**2.4)


def _math(nodes, links, operation, first, second=None):
    node = nodes.new('ShaderNodeMath')
    node.operation = operation
    for index, value in enumerate((first, second)):
        if value is None:
            continue
        if isinstance(value, (int, float)):
            node.inputs[index].default_value = value
        else:
            links.new(value, node.inputs[index])
    return node.outputs[0]


def _sky():
    mat = emissive('Dusty rose to butterscotch sky', (1, 1, 1, 1))
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    coord = nodes.new('ShaderNodeTexCoord')
    sep = nodes.new('ShaderNodeSeparateXYZ')
    links.new(coord.outputs['Generated'], sep.inputs[0])
    ramp = nodes.new('ShaderNodeValToRGB')
    ramp.color_ramp.interpolation = 'EASE'
    stops = [(0, (.55, .40, .34)), (.43, (.70, .52, .39)),
             (.64, (.63, .46, .43)), (1, (.46, .34, .42))]
    for i, (position, rgb) in enumerate(stops):
        stop = ramp.color_ramp.elements[i] if i < 2 else ramp.color_ramp.elements.new(position)
        stop.position = position
        stop.color = (*_linear(rgb), 1)
    links.new(sep.outputs['Z'], ramp.inputs['Fac'])

    # Radial shader masks: a pale 36px disc with a broad, very faint glow.
    # Position is on the front face of the sky cube (Y=246).
    geom = nodes.new('ShaderNodeNewGeometry')
    distance = nodes.new('ShaderNodeVectorMath')
    distance.operation = 'DISTANCE'
    links.new(geom.outputs['Position'], distance.inputs[0])
    distance.inputs[1].default_value = (440, 246, 260)
    masks = []
    for radius, strength, inner in ((108, .10, 0), (18, .42, .82)):
        radial = _math(nodes, links, 'DIVIDE', distance.outputs['Value'], radius)
        falloff = nodes.new('ShaderNodeValToRGB')
        falloff.color_ramp.interpolation = 'EASE'
        falloff.color_ramp.elements[0].position = inner
        falloff.color_ramp.elements[0].color = (1, 1, 1, 1)
        falloff.color_ramp.elements[1].color = (0, 0, 0, 1)
        links.new(radial, falloff.inputs['Fac'])
        masks.append(_math(nodes, links, 'MULTIPLY', falloff.outputs['Color'], strength))
    blend = nodes.new('ShaderNodeMixRGB')
    links.new(_math(nodes, links, 'ADD', *masks), blend.inputs[0])
    links.new(ramp.outputs['Color'], blend.inputs[1])
    blend.inputs[2].default_value = (*_linear((.85, .72, .58)), 1)
    links.new(blend.outputs[0], nodes.get('Emission').inputs['Color'])
    cube('Fully opaque sky', (0, 250, 0), (2100, 8, 1200), mat, bevel=0)


def _ridge(xs, seed, base, amplitude, mesa=False):
    rng = np.random.default_rng(seed)
    profile = np.zeros_like(xs, dtype=np.float64)
    for wavelength, weight in ((680, .62), (330, .30), (155, .14),
                                (73, .065), (35, .025)):
        phase = rng.uniform(0, wavelength)
        position = (xs+1400+phase)/wavelength
        index = np.floor(position).astype(int)
        fraction = position-index
        # Cubic interpolation of independent knots avoids angular polylines.
        smooth = fraction*fraction*(3-2*fraction)
        knots = rng.uniform(-1, 1, int(index.max())+2)
        profile += weight*(knots[index]*(1-smooth)+knots[index+1]*smooth)
    if mesa:
        # Broad caps keep their silhouette, but eroded tops never ruler-flat.
        cap = np.quantile(profile, .75)
        shoulder = np.clip((profile-cap+.025)/.05, 0, 1)
        shoulder = shoulder*shoulder*(3-2*shoulder)
        erosion = (1.1*np.sin(xs*.029+.6) + .65*np.sin(xs*.071+1.8)
                   - 1.6*np.exp(-((xs+448)/18)**2)) / amplitude
        # A soft minimum rounds the joins into gently sloping shoulders.
        profile = .5*(profile+cap-np.sqrt((profile-cap)**2+.008**2))
        profile += shoulder*erosion
        profile += .014*np.sin(xs*.034)*np.clip((cap-profile)*8, 0, 1)
    return base+amplitude*profile


def _range(layer, base, amplitude, color, haze_color):
    xs = np.arange(-1040, 1041, 8, dtype=np.float64)
    ridge = _ridge(xs, 3107+layer*113, base, amplitude, mesa=layer == 2)
    # Each column has vertical samples: the colour field carries broad haze
    # and gentle relief shading without stochastic transparent shader mixes.
    t = np.linspace(0, 1, 25)[:, None]
    z = -620+(ridge[None, :]+620)*t
    x = np.broadcast_to(xs[None, :], z.shape)
    y = np.full_like(z, 190-layer*40)
    verts = np.stack((x, y, z), axis=-1).reshape(-1, 3)
    n = len(xs)
    a = (np.arange(len(t)-1)[:, None]*n+np.arange(n-1)[None, :]).ravel()
    faces = np.stack((a, a+1, a+n+1, a+n), axis=-1)

    depth = ridge[None, :]-z
    fade = 1-np.exp(-np.maximum(depth, 0)/115)
    # Fog builds below the crest, then gently settles into a darker foreground.
    band = np.exp(-((z-(base-75))/78)**2)
    fog = np.clip((.30-.035*layer)*fade + (.12-.015*layer)*band, 0, .44)
    base_rgb = _linear(color)
    haze_rgb = _linear(haze_color)
    rgb = base_rgb[None, None, :]*(1-fog[..., None])+haze_rgb* fog[..., None]
    fluting = np.sin(x*.013+layer*1.9+.8*np.sin(z*.009))
    fluting += .35*np.sin(x*.038+z*.006+layer)
    variation = 1+.025*fluting*np.exp(-depth/240)
    rgb *= variation[..., None]
    # The low foreground is muted, without a black silhouette at the bottom.
    if layer == 4:
        rgb *= (1-.11*np.clip((-z-420)/200, 0, 1))[..., None]

    mat = emissive('Atmospheric range '+str(layer), (1, 1, 1, 1))
    obj = mesh('Haze washed ridge '+str(layer), verts.tolist(), faces.tolist(), mat)
    colors = obj.data.color_attributes.new(name='Atmosphere', type='FLOAT_COLOR', domain='POINT')
    rgba = np.ones((len(verts), 4), dtype=np.float32)
    rgba[:, :3] = rgb.reshape(-1, 3)
    colors.data.foreach_set('color', rgba.ravel())
    attr = mat.node_tree.nodes.new('ShaderNodeAttribute')
    attr.attribute_name = 'Atmosphere'
    mat.node_tree.links.new(attr.outputs['Color'],
                            mat.node_tree.nodes.get('Emission').inputs['Color'])


def build():
    _sky()
    for layer, settings in enumerate([
        (-35, 145, (.635, .477, .407), (.69, .515, .405)),
        (-135, 140, (.565, .409, .354), (.64, .468, .378)),
        (-240, 135, (.495, .349, .296), (.57, .410, .334)),
        (-350, 115, (.425, .303, .266), (.49, .349, .295)),
        (-445, 100, (.355, .263, .241), (.405, .298, .262)),
    ]):
        _range(layer, *settings)
    return lambda i: None


def postprocess(a):
    b = np.asarray(a, dtype=np.float32).copy()
    rng = np.random.default_rng(50831)
    # Display-space noise is shared by RGB, avoiding coloured dust speckles.
    # The pattern is deterministic in Blender's bottom-up array orientation.
    dither = (rng.random(b.shape[:2], dtype=np.float32)-.5)/255
    b[:, :, :3] = np.clip(b[:, :, :3]+dither[:, :, None], 0, 1)
    b[:, :, 3] = 1
    return b
