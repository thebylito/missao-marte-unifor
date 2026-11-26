"""Atlas em RGBA8, com acesso aos canais PNG sem transformação de cor."""
import json
import numpy as np
from specs import CATALOGO, SPECS


def ler_rgba(path):
    import bpy

    im = bpy.data.images.load(str(path), check_existing=False)
    try:
        im.alpha_mode = 'CHANNEL_PACKED'
        im.colorspace_settings.name = 'Non-Color'
        w, h = im.size
        data = np.empty(w*h*4, np.float32)
        im.pixels.foreach_get(data)
        return np.rint(data.reshape(h,w,4)[::-1]*255).astype(np.uint8)
    finally:
        bpy.data.images.remove(im)


def salvar_rgba(path, data):
    import bpy

    h, w = data.shape[:2]
    im = bpy.data.images.new(path.stem, width=w, height=h, alpha=True)
    try:
        im.alpha_mode = 'CHANNEL_PACKED'
        im.colorspace_settings.name = 'Non-Color'
        im.pixels.foreach_set(np.ascontiguousarray(data[::-1], dtype=np.float32).ravel()/255)
        im.file_format = 'PNG'
        im.filepath_raw = str(path)
        im.save()
    finally:
        bpy.data.images.remove(im)


def gerar_atlas(out):
    count = len(CATALOGO)
    previas = np.zeros(((count+7)//8*128, 8*128, 4), np.uint8)
    sombras = np.zeros((count*64, 16*64, 4), np.uint8)
    for i, entry in enumerate(CATALOGO):
        name = 'nave' if entry['id'] == 'padrao' else entry['id']
        folha = ler_rgba(out/SPECS[name].output)
        x, y = i%8*128, i//8*128
        previas[y:y+128, x:x+128] = folha[:128, :128]
        for j in range(16):
            frame = 4*j
            x, y = frame%8*128, frame//8*128
            cell = folha[y:y+128, x:x+128]
            mean = cell.reshape(64,2,64,2,4).mean(axis=(1,3))
            sombras[i*64:(i+1)*64, j*64:(j+1)*64] = np.floor(mean+.5)
    directory = out/'naves'
    directory.mkdir(exist_ok=True)
    salvar_rgba(directory/'previas.png', previas)
    salvar_rgba(directory/'sombras.png', sombras)
    (directory/'catalogo.json').write_text(json.dumps(CATALOGO, ensure_ascii=False, indent=2)+'\n')
