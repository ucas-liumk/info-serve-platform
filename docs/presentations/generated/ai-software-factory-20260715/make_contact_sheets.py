from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).parent / "previews"
for name in ("huashu", "frontend", "ppt-master"):
    files = sorted((ROOT / name).glob("slide-*.png"), key=lambda p: int(p.stem.split("-")[-1]))
    thumbs = []
    for file in files:
        im = Image.open(file).convert("RGB")
        im.thumbnail((400, 225))
        thumbs.append((file, im.copy()))
    cols, rows = 4, (len(thumbs) + 3) // 4
    sheet = Image.new("RGB", (cols * 420, rows * 255), "#E8EEF6")
    draw = ImageDraw.Draw(sheet)
    for idx, (file, im) in enumerate(thumbs):
        x, y = (idx % cols) * 420 + 10, (idx // cols) * 255 + 10
        sheet.paste(im, (x, y))
        draw.text((x, y + 228), file.stem, fill="#16324F")
    sheet.save(ROOT / f"{name}-contact-sheet.jpg", quality=88)
