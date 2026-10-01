# Demo recording preparation

Use this runbook before filming [hackathon-demo-video-script.md](hackathon-demo-video-script.md). No architectural changes—only environment, seed, and build settings.

## Quick pre-flight

```bash
# From repo root
supabase db reset
# Create Auth users (UUIDs in demo-data.md), then:
./supabase/seed/upload-assets.sh
./scripts/verify-demo-recording.sh   # optional regression + checklist
```

Record on a **Thursday** (visit weekday **4** in Meridian seed) or adjust seed visit weekday to match recording day. Run owner **approve** before the cleaner segment so Today generates a Meridian job.

## Checklist

### Demo account and data

| Check | How to verify |
|-------|----------------|
| Clean demo data | Fresh `supabase db reset` + seed applied |
| Predictable extraction | Owner → Contracts → **Meridian nightly clean** → review shows **same two** requirements every time |
| Predictable service job | After approve, cleaner **Today** lists **Meridian Lobby** job |
| Predictable evidence | `upload-assets.sh` uploaded; capture one photo if coverage still partial |
| Predictable dispute | Owner → Disputes → `44444444-4444-4444-8444-444444444401`; timeline non-empty |
| Deterministic report | Report preview loads; **Open PDF** works (seeded `reports` row + storage) |

### Recording hygiene

| Check | Action |
|-------|--------|
| No unexpected notifications | Do not enable push on recording device; avoid filing new disputes during record |
| No development-only screens | Skip onboarding/register; stay on golden script screens |
| No debug banners | Prefer release-like debug build; avoid showing offline banners unless scripted |
| No credential clutter | Set `DEMO_SHOW_CREDENTIALS=false` for polish, or crop login in edit |
| No crashes | Dry-run full script once before final take |
| Fast loading | Stable Wi‑Fi; Supabase project warm; use **logout** between roles instead of cold start |

### Edge / AI

| Check | Action |
|-------|--------|
| No live LLM surprises | Do **not** tap “Read contract again” or “Generate summary” |
| Optional safety net | Set `DEMO_DETERMINISTIC_EXTRACTION=true` on Edge Functions if you might re-extract |

## `local.properties` (recording debug APK)

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key

# Recording only (see demo-mode.md)
DEMO_SHOW_CREDENTIALS=true
DEMO_BYPASS_SUBSCRIPTION=true
```

Rebuild debug after changes. **Judge release APK** should not use bypass flags.

## APK choice

| APK | When |
|-----|------|
| **Debug** (`assembleDebug`) | Recording; faster iteration; optional demo flags |
| **Release** (`scripts/assemble-release-apk.sh`) | Matches judge install; RevenueCat Test Store |

## Device / emulator settings

- Pixel-class profile, API 34, **light theme**, portrait locked
- Resolution **1080×1920** for capture
- Do Not Disturb on
- Hide or crop emulator **DEBUG** ribbon in post if visible
- Disable “Show taps” unless intentional

## Golden-path validation (after reset)

- [ ] Meridian extraction review — two requirements
- [ ] Owner approve → cleaner Today — Meridian job
- [ ] Dispute detail — timeline populated; no missing-items banner
- [ ] Report preview + PDF
- [ ] Paywall visible when bypass is **off** (for subscription beat)

See [golden-path-hackathon.md](golden-path-hackathon.md).

## Related

- [demo-mode.md](../development/demo-mode.md)
- [demo-data.md](../development/demo-data.md)
- [screenshots.md](screenshots.md)
