#!/usr/bin/env python3
"""Raster launcher icons for ContractProof (original artwork, no external assets)."""
from __future__ import annotations

import math
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    raise SystemExit("Install Pillow: pip install Pillow")

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "docs" / "design"
RES = ROOT / "androidApp" / "src" / "main" / "res"

BG = (0xF4, 0xF6, 0xF8)
BG_SOFT = (0xD6, 0xE4, 0xF0)
PRIMARY = (0x1F, 0x4E, 0x79)
WHITE = (0xFF, 0xFF, 0xFF)
SUCCESS = (0x15, 0x73, 0x47)

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def draw_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), BG + (255,))
    draw = ImageDraw.Draw(img)
    cx, cy = size / 2, size / 2
    r_soft = int(size * 0.44)
    draw.ellipse(
        (cx - r_soft, cy - r_soft, cx + r_soft, cy + r_soft),
        fill=BG_SOFT + (255,),
    )

    s = size / 108.0

    def shield_polygon():
        return [
            (54 * s, 22 * s),
            (72 * s, 30 * s),
            (72 * s, 48 * s),
            (54 * s, 78 * s),
            (36 * s, 48 * s),
            (36 * s, 30 * s),
        ]

    draw.polygon(shield_polygon(), fill=PRIMARY + (255,))

    doc = [
        (46 * s, 38 * s),
        (62 * s, 38 * s),
        (62 * s, 62 * s),
        (46 * s, 62 * s),
    ]
    draw.polygon(doc, fill=WHITE + (255,))
    fold = [(58 * s, 38 * s), (62 * s, 42 * s), (58 * s, 42 * s)]
    draw.polygon(fold, fill=BG_SOFT + (255,))

    for y in (46, 50, 54):
        w = 60 if y < 54 else 56
        draw.rectangle(
            (48 * s, y * s, w * s, (y + 1) * s),
            fill=PRIMARY + (255,),
        )

    check = [
        (62 * s, 52 * s),
        (66 * s, 56 * s),
        (76 * s, 46 * s),
        (74 * s, 44 * s),
        (66 * s, 58 * s),
        (60 * s, 54 * s),
    ]
    draw.polygon(check, fill=SUCCESS + (255,))

    return img


def round_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    return mask


def main() -> None:
    DESIGN.mkdir(parents=True, exist_ok=True)
    master = draw_icon(1024)
    master.save(DESIGN / "icon-1024.png", "PNG", optimize=True)

    for folder, px in DENSITIES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = draw_icon(px)
        icon.save(out_dir / "ic_launcher.png", "PNG", optimize=True)
        round_icon = icon.copy()
        round_icon.putalpha(round_mask(px))
        round_icon.save(out_dir / "ic_launcher_round.png", "PNG", optimize=True)

    print(f"Wrote {DESIGN / 'icon-1024.png'} and legacy mipmaps under {RES}")


if __name__ == "__main__":
    main()
