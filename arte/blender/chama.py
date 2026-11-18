"""Four full exhaust plumes with a translucent halo and a short hot core.

One UV-shaded surface avoids stacked transparent shells and their bounce
limits in Cycles. Alpha comes from a shader, never a postprocess mask.
"""
import math

FLAME_LENGTHS = (58.0, 74.0, 85.0, 67.0)  # below attachment plane Z=48
ROWS, COLS = 33, 17


def flame_profile(frame, t):
    """Half-width and center in pixels; upper third stays on the engine axis.

    Width includes the halo. The alpha>0.5 body spans about 16 px at the
    mouth and 24-26 px at the shoulder, 15-24 px below the attachment.
    """
    if t <= .28:
        u = t / .28
        width = 10.2 + 5.8 * u * u * (3 - 2 * u)
    else:
        width = 16 * (1 - ((t - .28) / .72) ** 1.25) ** .8
    u = max(0, (t - 1 / 3) / (2 / 3))
    ease = u * u * (3 - 2 * u)
    wobble = (-3.8 * ease, 3.8 * ease, 0,
              3.2 * math.sin(2 * math.pi * u) * math.sin(math.pi * u))
    return width, wobble[frame % 4]


def flame_vertices(frame):
    """Sheet coordinates: nominal mouth Z=48 with 2 px of top overscan."""
    length = FLAME_LENGTHS[frame % 4]
    verts = []
    for row in range(ROWS):
        t = row / (ROWS - 1)
        width, wobble = flame_profile(frame, t)
        for col in range(COLS):
            across = 2 * col / (COLS - 1) - 1
            verts.append((wobble + across * width, 0, 50 - (length + 2) * t))
    return verts


def _flame_material():
    import bpy

    mat = bpy.data.materials.new('Procedural hot exhaust with transparent falloff')
    mat.use_nodes = True
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    nodes.clear()

    def math_node(operation, a, b=None):
        node = nodes.new('ShaderNodeMath')
        node.operation = operation
        for index, value in enumerate((a, b)):
            if value is None:
                continue
            if isinstance(value, (float, int)):
                node.inputs[index].default_value = value
            else:
                links.new(value, node.inputs[index])
        return node.outputs[0]

    def fade(value, start, end):
        node = nodes.new('ShaderNodeValToRGB')
        node.color_ramp.interpolation = 'EASE'
        first, last = node.color_ramp.elements
        first.position, first.color = start, (1, 1, 1, 1)
        last.position, last.color = end, (0, 0, 0, 1)
        links.new(value, node.inputs['Fac'])
        return node.outputs['Color']

    def mix(factor, first, second):
        node = nodes.new('ShaderNodeMixRGB')
        node.blend_type = 'MIX'
        links.new(factor, node.inputs[0])
        for index, value in ((1, first), (2, second)):
            if isinstance(value, tuple):
                node.inputs[index].default_value = value
            else:
                links.new(value, node.inputs[index])
        return node.outputs[0]

    uv = nodes.new('ShaderNodeTexCoord')
    split = nodes.new('ShaderNodeSeparateXYZ')
    links.new(uv.outputs['UV'], split.inputs[0])
    across = math_node('ABSOLUTE', math_node('SUBTRACT', math_node('MULTIPLY', split.outputs['X'], 2), 1))
    down = split.outputs['Y']
    body = math_node('MULTIPLY', fade(across, .62, .84), fade(down, .62, 1))
    # Composite a low-alpha orange envelope analytically on this one surface:
    # alpha = body + (1-body)*halo, with no extra transparent bounce layers.
    halo = math_node('MULTIPLY', .38,
                     math_node('MULTIPLY', fade(across, .82, 1), fade(down, .35, .96)))
    alpha = math_node('ADD', body,
                      math_node('MULTIPLY', math_node('SUBTRACT', 1, body), halo))
    inner = math_node('MULTIPLY', fade(across, .40, .82), fade(down, .50, .96))
    hot = math_node('MULTIPLY', fade(across, .12, .65), fade(down, .10, .30))
    envelope = mix(down, (1, .14, .008, 1), (.65, .022, .002, 1))
    gold = mix(inner, envelope, (1, .58, .035, 1))
    color = mix(hot, gold, (.78, .94, 1, 1))
    emission = nodes.new('ShaderNodeEmission')
    links.new(color, emission.inputs['Color'])
    emission.inputs['Strength'].default_value = .95
    transparent = nodes.new('ShaderNodeBsdfTransparent')
    shader = nodes.new('ShaderNodeMixShader')
    links.new(alpha, shader.inputs[0])
    links.new(transparent.outputs[0], shader.inputs[1])
    links.new(emission.outputs[0], shader.inputs[2])
    output = nodes.new('ShaderNodeOutputMaterial')
    links.new(shader.outputs[0], output.inputs['Surface'])
    return mat


def build_flame(frame=0, parent=None):
    """Return attachment pivot + animation callback, mouth at local Z=0.

    Reused by the icon; all lengths and widths remain in ship pixel units.
    """
    from common import root, mesh

    pivot = root('Exhaust mouth attachment')
    if parent is not None:
        pivot.parent = parent

    def local_vertices(i):
        return [(x, y, z - 48) for x, y, z in flame_vertices(i)]
    faces = []
    for row in range(ROWS - 1):
        for col in range(COLS - 1):
            a = row * COLS + col
            faces.append((a, a + 1, a + COLS + 1, a + COLS))
    flame = mesh('Soft procedural exhaust ribbon', local_vertices(frame), faces,
                 _flame_material(), pivot)
    uv = flame.data.uv_layers.new(name='Exhaust cross section and distance')
    for loop in flame.data.loops:
        row, col = divmod(loop.vertex_index, COLS)
        uv.data[loop.index].uv = (col / (COLS - 1), row / (ROWS - 1))

    def animate(i):
        for vertex, position in zip(flame.data.vertices, local_vertices(i)):
            vertex.co = position
        flame.data.update()

    return pivot, animate


def build():
    pivot, animate = build_flame()
    pivot.location.z = 48
    return animate
