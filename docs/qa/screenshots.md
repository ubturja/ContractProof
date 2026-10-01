# Hackathon screenshots

Polished captures for README, slides, and Play Console. Use **fictional ClearLine demo data** only; never real API keys, production URLs, or personal information.

## Image sizes

| Use | Dimensions | Format |
|-----|------------|--------|
| Play phone screenshots | **1080×1920** or **1440×2560** (9:16) | PNG |
| Play feature graphic | **1024×500** | PNG |
| GitHub README | Same as phone; display width ~600–800 px in markdown | PNG |
| Optional slides | 16:9 crop of phone or separate art | PNG |

Repository paths:

- `docs/assets/screenshots/01-dashboard.png` … `07-paywall.png`
- `docs/assets/screenshots/feature-graphic-1024x500.png`

## Reference vs device capture

| Source | When |
|--------|------|
| `python3 scripts/generate_reference_screenshots.py` | CI / no emulator — brand-colored frames with demo copy |
| `scripts/capture-screenshots.sh` | Preferred — real app UI from emulator after golden path |

Requires local [platform-tools](https://developer.android.com/tools/releases/platform-tools) at `.tools/platform-tools/adb` (or system `adb`).

## Environment

1. `supabase db reset` and apply [`seed.sql`](../../supabase/seed.sql)
2. `./supabase/seed/upload-assets.sh`
3. Debug APK with Supabase URL/anon key from [`local.properties.example`](../../local.properties.example)
4. Emulator: Pixel 6 or 7, API 34, light theme
5. Turn **off** `DEMO_SHOW_CREDENTIALS`, `DEMO_BYPASS_SUBSCRIPTION` for public images (show real paywall on Free plan)

## Seven screens

| File | Story | Role / route | Golden-path step |
|------|-------|--------------|------------------|
| `01-dashboard.png` | Owner overview | `owner@clearline.demo` → Dashboard | [golden-path § step 1](golden-path-hackathon.md) |
| `02-contract-extraction.png` | AI extraction review | Contracts → Meridian nightly clean → Review | Steps 2–4 ([final-golden-flow.md](final-golden-flow.md)) |
| `03-cleaner-service.png` | Cleaner today | `cleaner@clearline.demo` → Today | Step 5 |
| `04-evidence-coverage.png` | Evidence completeness | Job coverage | Step 7 |
| `05-dispute-reconstruction.png` | Dispute timeline | Disputes → Northstar dispute | Steps 9–10 |
| `06-evidence-report.png` | PDF / preview | Report preview | Step 11 |
| `07-paywall.png` | Subscription gate | Paywall / settings plan | Step 12 |

## adb capture (single frame)

```bash
adb exec-out screencap -p > docs/assets/screenshots/01-dashboard.png
```

Resize if needed (ImageMagick):

```bash
magick input.png -resize 1080x1920! output.png
```

## Checklist before publish

- [ ] No `local.properties` values visible
- [ ] Only `*.clearline.demo` accounts if login is shown
- [ ] Status bar free of debug overlays
- [ ] Copy matches seeded clients (Meridian, Northstar, Westbridge)

## Related

- [golden-path-hackathon.md](golden-path-hackathon.md)
- [demo-data.md](../development/demo-data.md)
- [app-icon.md](../design/app-icon.md)
