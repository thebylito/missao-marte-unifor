"""Run only inside Blender. Verify disk PNGs, independently of render state."""
import sys
sys.dont_write_bytecode = True
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import importlib, json, re, struct
import numpy as np
from specs import SPECS, SKINS, CATALOGO
from naves.atlas import ler_rgba
OUT = Path(__file__).resolve().parent.parent.parent / 'assets' / 'sprites'
EVOLUTIONS = {item['id']: item for item in CATALOGO if item['nivel'] > 1}
LEVELS = {(item['linha'], item['nivel']): item['id'] for item in CATALOGO}

# Four RGBA8 code values on average correspond, for example, to a change
# of 40 per channel across 10% of the shared opaque area.
MIN_RGB_DIFFERENCE = 4.0

def pixels(path):
    import bpy

    im = bpy.data.images.load(str(path), check_existing=False)
    w,h = im.size
    p = np.empty(w*h*4, np.float32)
    im.pixels.foreach_get(p)
    bpy.data.images.remove(im)
    return p.reshape(h,w,4)[::-1].copy()

def png_header(path, size):
    with path.open('rb') as stream:
        header = stream.read(33)
    assert header[:8] == b'\x89PNG\r\n\x1a\n', 'not PNG'
    w,h,depth,color = struct.unpack('>IIBB', header[16:26])
    assert (w,h) == size, f'wrong dimensions {w}x{h}'
    assert (depth,color) == (8,6), f'expected 8-bit RGBA, got {depth}/{color}'


def silhouette(alpha):
    y,x = np.where(alpha > .5)
    return len(y), np.array([x.min(),y.min(),x.max(),y.max()])


def verify_skin(alpha, reference, module):
    area, box = silhouette(alpha)
    base_area, base_box = reference
    assert .8 <= area/base_area <= 1.2, f'area {area}/{base_area}'
    assert np.max(np.abs(box-base_box)) <= 10, f'bounds {box} vs {base_box}'
    for x,y in [(40,104),(88,104),(64,92)]:
        assert alpha[y-1,x] > .5 and alpha[y+1,x] < .5, f'landing gear or nozzle at {x},{y}'
    radius, part = importlib.import_module(module).geometry_radius_bound()
    assert radius < 56, f'radius {radius} at {part}'


def verify_evolution(previous, current):
    before = previous[:, :, 3] > 127
    after = current[:, :, 3] > 127
    shared = before & after
    union = np.count_nonzero(before | after)
    overlap = np.count_nonzero(shared) / union if union else 0.0
    assert overlap >= .6, f'silhouette IoU {overlap:.3f} < 0.6'
    difference = np.abs(current[shared, :3].astype(np.int16) - previous[shared, :3])
    mean_rgb = float(difference.mean())
    assert mean_rgb > MIN_RGB_DIFFERENCE, (
        f'mean RGB difference {mean_rgb:.2f} <= {MIN_RGB_DIFFERENCE:g} (8-bit RGB)'
    )
    return {'iou': overlap, 'meanRgbDifference': mean_rgb}


def verify_catalog():
    catalog = json.loads((OUT/'naves/catalogo.json').read_text())
    assert catalog == CATALOGO, 'catalog differs from spec'
    ids = [item['id'] for item in catalog]
    assert ids[0] == 'padrao' and len(set(ids)) == len(ids), 'default or duplicate id'
    previous = None
    for item in catalog:
        assert set(item) == {'id','nome','descricao','linha','nivel'}, 'catalog fields'
        assert re.fullmatch('[a-z0-9_]+', item['id']), 'invalid id'
        assert item['nome'].strip() and item['descricao'].strip(), 'missing text'
        level = item['nivel']
        suffix = '' if level == 1 else f'_{level}'
        assert type(level) is int and level >= 1 and item['id'] == item['linha'] + suffix, 'invalid level or line'
        if level > 1:
            assert previous and (previous['linha'], previous['nivel']) == (item['linha'], level-1), 'missing predecessor'
        previous = item
    return ids


def verify_atlas(name, ids):
    preview = name == 'previas'
    h = ((len(ids)+7)//8*128) if preview else len(ids)*64
    path = OUT/'naves'/(name+'.png')
    png_header(path, (1024,h))
    atlas = ler_rgba(path)
    for i, skin in enumerate(ids):
        sheet = ler_rgba(OUT/SPECS['nave' if skin == 'padrao' else skin].output)
        if preview:
            y,x = i//8*128,i%8*128
            assert np.array_equal(atlas[y:y+128,x:x+128],sheet[:128,:128]), skin
        else:
            for j in range(16):
                y,x = divmod(j*4,8)
                cell = sheet[y*128:(y+1)*128,x*128:(x+1)*128].astype(np.uint16)
                expected = (cell[::2,::2]+cell[1::2,::2]+cell[::2,1::2]+cell[1::2,1::2]+2)//4
                assert np.array_equal(atlas[i*64:(i+1)*64,j*64:(j+1)*64],expected), f'{skin} frame {j*4}'
    if preview and len(ids)%8:
        assert not atlas[-128:,len(ids)%8*128:].any(), 'preview padding'


def verify():
    errors = []
    results = []
    manifest = json.loads((OUT/'manifest.json').read_text()) if (OUT/'manifest.json').exists() else {}
    print('\nASSET                       SIZE          FRAMES  RESULT', flush=True)
    reference = silhouette(pixels(OUT/'nave.png')[:128,:128,3])
    for name,spec in SPECS.items():
        w,h,n,c,r = spec.grid
        path = OUT/spec.output
        evolution = None
        try:
            assert path.exists(), 'missing file'
            png_header(path,(c*w,r*h))
            a = pixels(path)
            entry = manifest['assets'][name]
            for key,val in [('file',spec.output),('cellW',w),('cellH',h),('cols',c),('rows',r),('frames',n)]:
                assert entry[key]==val, f'manifest {key}'
            seen = []
            for i in range(n):
                cell=a[(i//c)*h:(i//c+1)*h,(i%c)*w:(i%c+1)*w]
                alpha=cell[:,:,3]
                assert alpha.max()>.1, f'empty frame {i}'
                if name not in ('rocha_tile','fundo'):
                    assert max(alpha[0,0],alpha[0,-1],alpha[-1,0],alpha[-1,-1])==0, f'corners frame {i}'
                    assert np.any((alpha>0)&(alpha<1)), f'no antialiasing frame {i}'
                else:
                    assert alpha.min()==1, 'opaque fill required'
                if name in SKINS or name in ('nave','asteroide','explosao','icone','inimigo'):
                    assert max(alpha[0].max(),alpha[-1].max(),alpha[:,0].max(),alpha[:,-1].max())==0, f'clipping frame {i}'
                if name in ('astronauta','engenheiro','professor'):
                    assert alpha[-1].max()>.5, f'feet do not reach bottom {i}'
                if name=='chama':
                    assert alpha[0,w//2]>.5, f'mouth not at top-center {i}'
                if name.startswith('plataforma_'):
                    y=16 if name=='plataforma_base' else 12
                    assert alpha[y,w//2]>.5 and alpha[y-1,w//2]<.5, 'landing line mismatch'
                seen.append(cell.tobytes())
            assert len(set(seen))==n, 'duplicate animation frames'
            if name=='rocha_tile':
                dx=float(np.abs(a[:,0]-a[:,-1]).max())
                dy=float(np.abs(a[0]-a[-1]).max())
                assert dx==0 and dy==0, f'tile seam {dx}/{dy}'
            if name in SKINS:
                verify_skin(a[:h,:w,3], reference, spec.module)
            if name=='nave' or name in SKINS:
                # Cyan cockpit should be above/below/left/right at the cardinal frames.
                for idx,axis,sign in [(0,0,-1),(16,1,-1),(32,0,1),(48,1,1)]:
                    cell=a[(idx//c)*h:(idx//c+1)*h,(idx%c)*w:(idx%c+1)*w]
                    mask=(cell[:,:,2]>cell[:,:,0]*1.4)&(cell[:,:,1]>cell[:,:,0]*1.3)&(cell[:,:,3]>.7)
                    yy,xx=np.where(mask)
                    assert len(yy)>3, 'missing cyan cockpit'
                    coord=(yy.mean()-h/2) if axis==0 else (xx.mean()-w/2)
                    assert coord*sign>3, f'orientation frame {idx}'
            if name in EVOLUTIONS:
                item = EVOLUTIONS[name]
                previous = LEVELS[item['linha'], item['nivel']-1]
                previous_spec = SPECS['nave' if previous == 'padrao' else previous]
                try:
                    evolution = verify_evolution(
                        ler_rgba(OUT/previous_spec.output)[:h, :w],
                        ler_rgba(path)[:h, :w],
                    )
                except AssertionError as ex:
                    raise AssertionError(f'evolution from {previous}: {ex}') from ex
                evolution['previous'] = previous
            status='PASS'
        except Exception as ex:
            status='FAIL: '+str(ex)
            errors.append(name+': '+str(ex))
        print(f'{spec.output:27} {c*w:4} x {r*h:<4}  {n:3}    {status}', flush=True)
        results.append({'asset':name,'size':[c*w,r*h],'frames':n,'result':status})
        if evolution is not None:
            results[-1]['evolution'] = evolution
    for name in ('catalogo.json','previas.png','sombras.png'):
        try:
            ids = verify_catalog()
            if name.endswith('.png'):
                verify_atlas(Path(name).stem, ids)
            status = 'PASS'
        except Exception as ex:
            status = 'FAIL: '+str(ex)
            errors.append(name+': '+str(ex))
        print(f'naves/{name:40} {status}', flush=True)
        results.append({'asset':'naves/'+name,'result':status})
    report={'passed':not errors,'results':results,'errors':errors}
    (Path(__file__).parent/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
    if errors:
        raise RuntimeError('; '.join(errors))
    print('PASS: dimensions, RGBA, frame contents, corners, clipping, pivots/landing lines, orientation, tile edges, catalog, atlases, and evolution.',flush=True)
    return report

if __name__=='__main__':
    verify()
