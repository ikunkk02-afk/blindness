"""Generate the original Blindness guidance-cane texture and mod icon."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ITEM = ROOT / "src/main/resources/assets/blindness/textures/item/guidance_cane.png"
ICON = ROOT / "src/main/resources/assets/blindness/icon.png"

image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = image.load()

# Soft cyan echo fragments, deliberately sparse so the item silhouette stays clear.
for x, y in [(2, 7), (2, 8), (3, 5), (4, 4), (6, 3), (7, 3), (9, 4), (11, 6),
             (12, 7), (12, 8), (11, 10), (9, 12), (7, 13), (5, 12), (3, 10)]:
    px[x, y] = (94, 225, 239, 135)

# White/gray shaft with a dark edge, running bottom-left to top-right.
for x, y in [(3, 13), (4, 12), (5, 11), (6, 10), (7, 9), (8, 8), (9, 7), (10, 6), (11, 5)]:
    px[x, y] = (48, 52, 57, 255)
    if y - 1 >= 0:
        px[x, y - 1] = (236, 239, 238, 255)
    if x + 1 < 16:
        px[x + 1, y] = (146, 153, 157, 255)

# Ergonomic hooked handle.
for x, y in [(11, 4), (12, 3), (13, 3), (14, 4), (14, 5), (13, 6), (12, 5)]:
    px[x, y] = (38, 42, 47, 255)
px[12, 4] = (84, 89, 94, 255)
px[13, 4] = (67, 71, 76, 255)

# Red safety tip.
for x, y in [(2, 14), (3, 14), (2, 13), (3, 12)]:
    px[x, y] = (224, 48, 47, 255)
px[1, 15] = (43, 47, 52, 255)
px[2, 15] = (132, 27, 28, 255)

ITEM.parent.mkdir(parents=True, exist_ok=True)
ICON.parent.mkdir(parents=True, exist_ok=True)
image.save(ITEM)
image.resize((64, 64), Image.Resampling.NEAREST).save(ICON)
print(f"generated {ITEM} and {ICON}")
