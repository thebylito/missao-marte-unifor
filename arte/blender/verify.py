"""Run only inside Blender. Verify disk PNGs, independently of render state."""
import sys
sys.dont_write_bytecode = True
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import json, struct
import bpy
import numpy as np
from specs import SPECS
OUT = Path(__file__).resolve().parent.parent.parent / 'assets' / 'sprites'

def pixels(path):
    im = bpy.data.images.load(str(path), check_existing=False)
    w,h = im.size
    p = np.empty(w*h*4, np.float32)
    im.pixels.foreach_get(p)
    bpy.data.images.remove(im)
    return p.reshape(h,w,4)[::-1].copy()

def verify():
    errors = []
    results = []
    manifest = json.loads((OUT/'manifest.json').read_text()) if (OUT/'manifest.json').exists() else {}
    print('\nASSET                       SIZE          FRAMES  RESULT', flush=True)
    for name,(w,h,n,c,r) in SPECS.items():
        path = OUT/(name+'.png')
        try:
            assert path.exists(), 'missing file'
            header = path.read_bytes()[:33]
            assert header[:8] == b'\x89PNG\r\n\x1a\n', 'not PNG'
            width,height,depth,color = struct.unpack('>IIBB', header[16:26])
            assert (width,height)==(c*w,r*h), f'wrong dimensions {width}x{height}'
            assert color==6 and depth==8, f'expected 8-bit RGBA, got {depth}/{color}'
            a = pixels(path)
            entry = manifest['assets'][name]
            for key,val in [('file',name+'.png'),('cellW',w),('cellH',h),('cols',c),('rows',r),('frames',n)]:
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
                if name in ('nave','asteroide','explosao','icone','inimigo'):
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
            if name=='nave':
                # Cyan cockpit should be above/below/left/right at the cardinal frames.
                for idx,axis,sign in [(0,0,-1),(16,1,-1),(32,0,1),(48,1,1)]:
                    cell=a[(idx//c)*h:(idx//c+1)*h,(idx%c)*w:(idx%c+1)*w]
                    mask=(cell[:,:,2]>cell[:,:,0]*1.4)&(cell[:,:,1]>cell[:,:,0]*1.3)&(cell[:,:,3]>.7)
                    yy,xx=np.where(mask)
                    assert len(yy)>3, 'missing cyan cockpit'
                    coord=(yy.mean()-h/2) if axis==0 else (xx.mean()-w/2)
                    assert coord*sign>3, f'orientation frame {idx}'
            status='PASS'
        except Exception as ex:
            status='FAIL: '+str(ex)
            errors.append(name+': '+str(ex))
        print(f'{name+".png":27} {c*w:4} x {r*h:<4}  {n:3}    {status}', flush=True)
        results.append({'asset':name,'size':[c*w,r*h],'frames':n,'result':status})
    report={'passed':not errors,'results':results,'errors':errors}
    (Path(__file__).parent/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
    if errors:
        raise RuntimeError('; '.join(errors))
    print('PASS: dimensions, RGBA, frame contents, corners, clipping, pivots/landing lines, orientation, tile edges (0 difference).',flush=True)
    return report

if __name__=='__main__':
    verify()
