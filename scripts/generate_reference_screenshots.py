#!/usr/bin/env python3
"""Reference phone frames for hackathon docs when device capture is unavailable.

Uses ClearLine demo copy and ContractProof colors only (no app bitmaps).
Replace with scripts/capture-screenshots.sh when an emulator is available.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "assets" / "screenshots"
W, H = 1080, 1920

PRIMARY = (0x1F, 0x4E, 0x79)
BG = (0xF4, 0xF6, 0xF8)
SURFACE = (0xFF, 0xFF, 0xFF)
TEXT = (0x1B, 0x1F, 0x24)
MUTED = (0x3D, 0x46, 0x54)
SUCCESS = (0x15, 0x73, 0x47)
OUTLINE = (0xD0, 0xD7, 0xDE)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    paths = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/TTF/DejaVuSans.ttf",
    ]
    for p in paths:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def base(title: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, 160), fill=SURFACE)
    d.rectangle((0, 158, W, 160), fill=OUTLINE)
    d.text((48, 72), title, fill=TEXT, font=font(52, True))
    d.text((48, 168), "ClearLine Facility Services", fill=MUTED, font=font(28))
    return img, d


def card(d: ImageDraw.ImageDraw, y: int, h: int, lines: list[str]) -> None:
    d.rounded_rectangle((48, y, W - 48, y + h), radius=24, fill=SURFACE, outline=OUTLINE, width=2)
    ty = y + 32
    for i, line in enumerate(lines):
        d.text((72, ty + i * 44), line, fill=TEXT if i == 0 else MUTED, font=font(34 if i == 0 else 28, i == 0))


SCREENS = [
    (
        "01-dashboard.png",
        "Dashboard",
        lambda img, d: (
            card(d, 240, 200, ["Today's jobs", "Meridian Lobby — in progress"]),
            card(d, 480, 200, ["Open disputes", "Northstar weekly service"]),
            card(d, 720, 180, ["Contracts", "3 active · 1 needs review"]),
        ),
    ),
    (
        "02-contract-extraction.png",
        "Review extraction",
        lambda img, d: (
            card(d, 240, 120, ["Meridian nightly clean", "Version ready for review"]),
            card(d, 400, 160, ["Sanitize exam rooms", "Mandatory · weekly"]),
            card(d, 600, 160, ["Empty clinical waste bins", "Mandatory · weekly"]),
            d.text((72, 820), "Approve to create task requirements", fill=PRIMARY, font=font(28)),
        ),
    ),
    (
        "03-cleaner-service.png",
        "Today",
        lambda img, d: (
            card(d, 240, 220, ["Meridian Lobby", "Scheduled · assigned to you"]),
            d.text((72, 520), "Start job → tasks → capture photo evidence", fill=MUTED, font=font(28)),
        ),
    ),
    (
        "04-evidence-coverage.png",
        "Coverage",
        lambda img, d: (
            card(d, 240, 140, ["Mandatory tasks", "4 of 4 complete"]),
            d.rectangle((72, 420, W - 72, 460), fill=OUTLINE),
            d.rectangle((72, 420, 72 + int((W - 144) * 0.85), 460), fill=SUCCESS),
            d.text((72, 500), "85% evidence coverage", fill=SUCCESS, font=font(36, True)),
        ),
    ),
    (
        "05-dispute-reconstruction.png",
        "Dispute",
        lambda img, d: (
            card(d, 240, 120, ["Northstar Dock Office", "Status: open"]),
            card(d, 400, 280, ["Timeline", "Dock loading bay — photo uploaded"]),
            d.text((96, 520), "Break room — exception logged", fill=MUTED, font=font(26)),
            d.text((96, 580), "AI summary cached from seed", fill=MUTED, font=font(26)),
        ),
    ),
    (
        "06-evidence-report.png",
        "Report preview",
        lambda img, d: (
            card(d, 240, 400, ["Evidence report", "Job summary · requirements met"]),
            d.text((72, 700), "Open PDF — share with client", fill=PRIMARY, font=font(32)),
        ),
    ),
    (
        "07-paywall.png",
        "Upgrade",
        lambda img, d: (
            card(d, 280, 360, ["ContractProof Pro", "Unlimited extraction & dispute PDFs"]),
            d.rounded_rectangle((120, 720, W - 120, 840), radius=16, fill=PRIMARY),
            d.text((W // 2 - 120, 760), "Subscribe", fill=SURFACE, font=font(36, True)),
        ),
    ),
]


def feature_graphic() -> None:
    img = Image.new("RGB", (1024, 500), PRIMARY)
    d = ImageDraw.Draw(img)
    d.text((48, 180), "ContractProof", fill=SURFACE, font=font(72, True))
    d.text((48, 280), "Contract evidence for facility services", fill=(0xD6, 0xE4, 0xF0), font=font(32))
    img.save(OUT / "feature-graphic-1024x500.png", "PNG", optimize=True)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for filename, title, painter in SCREENS:
        img, d = base(title)
        painter(img, d)
        img.save(OUT / filename, "PNG", optimize=True)
    feature_graphic()
    print(f"Wrote reference screenshots to {OUT}")


if __name__ == "__main__":
    main()
