"""Regenerate all assets with Blender bpy; compose only with bundled numpy and images."""
import sys
sys.dont_write_bytecode=True
from pathlib import Path
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE))
import argparse, importlib, json, os, time
import numpy as np
from specs import SPECS, SKINS
from naves.atlas import gerar_atlas
from verify import verify

OUT=HERE.parent.parent/'assets'/'sprites'   # raiz do projeto: assets/sprites


def arguments():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--engine',choices=['eevee','cycles'],default='eevee')
    p.add_argument('--samples',type=int,default=16)
    p.add_argument('--only',nargs='+',choices=list(SPECS))
    mode = p.add_mutually_exclusive_group()
    mode.add_argument('--render-only',action='store_true',help='write sprite sheets only')
    mode.add_argument('--atlas-only',action='store_true',help='compose atlases and metadata from disk sheets')
    mode.add_argument('--verify-only',action='store_true',help='verify the full asset set on disk')
    args = p.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    if args.only and (args.atlas_only or args.verify_only):
        p.error('--only is for render modes')
    return args


def metadata():
    data={'version':1,'title':'Missão Marte','imageOrigin':'top-left',
          'frameOrder':'left-to-right, top-to-bottom','alphaMode':'straight',
          'camera':'orthographic XZ','lighting':'fixed upper-left key + subtle rim',
          'assets':{}}
    for name, spec in SPECS.items():
        w,h,n,c,r = spec.grid
        entry={'file':spec.output,'cellW':w,'cellH':h,'cols':c,'rows':r,'frames':n,
               'width':w*c,'height':h*r,'pivot':{'x':w/2,'y':0 if name=='chama' else h/2},
               'opaque':name in ('fundo','rocha_tile','parede_gelo','parede_ferro','fundo_polar','fundo_ferro')}
        if name=='nave' or name in SKINS:
            entry.update(frame0='nose up',rotationDirection='counterclockwise',degreesPerFrame=5.625)
        if name.startswith('plataforma_'):
            entry['landingY']=16 if name=='plataforma_base' else 12
        if name in ('astronauta','engenheiro','professor'):
            entry['feetY']=h
            entry['loop']=True
        if name=='inimigo':
            entry['loop']=True
        if name=='asteroide':
            entry.update(degreesPerFrame=11.25,rotationAxis=[.36,.84,.40],loop=True)
        if name=='rocha_tile':
            entry['tileable']=True
        if name=='sentinela':
            entry.update(pivot={'x':64,'y':118},pivotName='fixed ground contact',
                         loop=True,animation='stationary base; rotating scanning head')
        if name=='antena_missao':
            entry.update(pivot={'x':128,'y':178},pivotName='fixed ground contact',
                         states={'off':[0,3],'activating':[4,7],'active':[8,11]},
                         animation='stationary base; reflector orientation and transmitting dipole')
        data['assets'][name]=entry
    return data


def write_manifest():
    path = OUT/'manifest.json'
    data = metadata()
    if path.exists():
        # Preserve entries for previously published sheets, even when retired.
        previous = json.loads(path.read_text())
        data['assets'] = {**previous['assets'], **data['assets']}
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')


def read_render(path,w,h):
    import bpy

    im=bpy.data.images.load(str(path),check_existing=False)
    im.alpha_mode='STRAIGHT'
    a=np.empty(w*h*4,np.float32)
    im.pixels.foreach_get(a)
    bpy.data.images.remove(im)
    return a.reshape(h,w,4)


def write_sheet(path,a):
    import bpy

    h,w=a.shape[:2]
    im=bpy.data.images.new(path.stem,width=w,height=h,alpha=True,float_buffer=False)
    im.alpha_mode='STRAIGHT'
    im.colorspace_settings.name='sRGB'
    im.pixels.foreach_set(np.ascontiguousarray(a).ravel())
    im.file_format='PNG'
    im.filepath_raw=str(path)
    im.save()
    bpy.data.images.remove(im)


def render_asset(name,args,frame_path):
    import bpy
    from common import scene

    start=time.perf_counter()
    spec = SPECS[name]
    w,h,n,c,r = spec.grid
    s=scene(w,h,opaque=name in ('fundo','rocha_tile','parede_gelo','parede_ferro','fundo_polar','fundo_ferro'),engine=args.engine,samples=args.samples)
    module=importlib.import_module(spec.module)
    animate=module.build()
    sheet=np.zeros((r*h,c*w,4),np.float32)
    engines=set()
    for i in range(n):
        animate(i)
        bpy.context.view_layer.update()
        s.render.filepath=str(frame_path)
        try:
            bpy.ops.render.render(write_still=True)
        except RuntimeError:
            if s.render.engine=='CYCLES':
                raise
            print('EEVEE render failed; retrying Cycles CPU at low samples.',flush=True)
            s.render.engine='CYCLES'
            s.cycles.device='CPU'
            s.cycles.samples=args.samples
            s.cycles.use_denoising=False
            s.cycles.max_bounces=3
            bpy.ops.render.render(write_still=True)
        engines.add(s.render.engine)
        a=read_render(frame_path,w,h)
        if hasattr(module,'postprocess'):
            a=module.postprocess(a)
        a[a[:,:,3]==0,:3]=0
        # Blender pixels start bottom-left; contract sheet frames start top-left.
        y=(r-1-i//c)*h
        x=(i%c)*w
        sheet[y:y+h,x:x+w]=a
        print(f'FRAME {name} {i+1}/{n}',flush=True)
    path = OUT/spec.output
    path.parent.mkdir(parents=True, exist_ok=True)
    write_sheet(path,sheet)
    elapsed=time.perf_counter()-start
    print(f'ASSET {name}: {c*w}x{r*h}; {elapsed:.2f}s',flush=True)
    return {'seconds':round(elapsed,3),'engines':sorted(engines),'frames':n}


def main():
    args=arguments()
    if args.samples<1:
        raise ValueError('--samples must be positive')
    if args.verify_only:
        verify()
        return
    if args.atlas_only:
        gerar_atlas(OUT)
        write_manifest()
        return
    import bpy

    start=time.perf_counter()
    OUT.mkdir(parents=True,exist_ok=True)
    frame_path=HERE/f'.render_frame_{os.getpid()}.png'  # um por processo: permite renders em paralelo
    report={'blenderVersion':bpy.app.version_string,'assets':{},'complete':False}
    try:
        for name in args.only or SPECS:
            report['assets'][name]=render_asset(name,args,frame_path)
        if not args.render_only:
            gerar_atlas(OUT)
            write_manifest()
        report['renderSeconds']=round(time.perf_counter()-start,3)
        report['underTenMinutes']=report['renderSeconds']<600
        if not args.render_only:
            # Full-set verification also runs after a partial rerender by default.
            verify()
        report['complete']=True
    finally:
        report['totalSeconds']=round(time.perf_counter()-start,3)
        (HERE/'render_report.json').write_text(json.dumps(report,indent=2)+'\n')
        frame_path.unlink(missing_ok=True)
    print(f'TOTAL RENDER {report["renderSeconds"]:.2f}s; TOTAL {report["totalSeconds"]:.2f}s',flush=True)
    if not report['underTenMinutes']:
        print('DEVIATION: regeneration exceeded the 600-second target.',flush=True)


if __name__=='__main__':
    main()
