"""Execute no Blender em --background para conferir as folhas do capítulo."""
import sys
import json
from pathlib import Path
import bpy
import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
sys.path.insert(0, str(ROOT / 'arte/blender'))
from verify import pixels

results = {}
for name, width, height, count, cols, pivot, stable_y in (
    ('sentinela', 128, 128, 16, 4, (64, 118), 92),
    ('antena_missao', 256, 192, 12, 4, (128, 178), 133),
):
    sheet = pixels(ROOT / 'assets/sprites' / (name + '.png'))
    frames = [sheet[i // cols * height:(i // cols + 1) * height,
                    i % cols * width:(i % cols + 1) * width] for i in range(count)]
    bounds = []
    for frame in frames:
        alpha = frame[:, :, 3]
        assert max(alpha[0].max(), alpha[-1].max(), alpha[:, 0].max(), alpha[:, -1].max()) == 0
        assert np.any((alpha > 0) & (alpha < 1))
        y, x = np.where(alpha > .5)
        bounds.append([int(x.min()), int(y.min()), int(x.max()), int(y.max())])
    assert len({frame.tobytes() for frame in frames}) == count
    stable_base = all(np.array_equal(frame[stable_y:, :, 3], frames[0][stable_y:, :, 3]) for frame in frames)
    assert stable_base, name + ': a silhueta da base mudou'
    assert all(bounds[0][3] == box[3] for box in bounds)
    results[name] = {
        'frames': count, 'uniqueFrames': count, 'allBordersTransparent': True,
        'allFramesAntialiased': True, 'fixedBaseAlphaFromRow': stable_y,
        'fixedBaseSilhouette': stable_base, 'pivotTopLeft': list(pivot),
        'opaqueBoundsPerFrame': bounds,
    }
    if name == 'antena_missao':
        # Narrow state strip in the fixed mast, independently of the moving dish.
        colors = [np.mean(frame[118:130, 125:131, :3], axis=(0, 1)).tolist() for frame in frames]
        assert colors[8][1] > colors[8][0] * 1.4
        assert colors[4][0] > colors[4][2] * 1.4
        results[name]['mastMeanRGBPerFrame'] = colors
        results[name]['states'] = {'off': [0, 3], 'activating': [4, 7], 'active': [8, 11]}

report = {'passed': True, 'checks': results}
(HERE / 'geometria.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
