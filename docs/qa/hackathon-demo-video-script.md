# Hackathon demo video script (under 2 minutes)

**Target length:** 1:50–1:58  
**Platform:** Android (device or emulator), portrait 1080×1920  
**Story arc:** Problem → Contract → AI extraction → Service → Evidence → Dispute → Evidence report → Subscription

Judges should follow the product from **on-screen UI and short overlays** without narration on every tap. **Canonical flow:** [final-golden-flow.md](final-golden-flow.md). Use [demo-recording-prep.md](demo-recording-prep.md) before recording.

## Accounts (ClearLine seed)

| Role | Email | Password |
|------|--------|----------|
| Owner | `owner@clearline.demo` | See [demo-data.md](../development/demo-data.md) |
| Cleaner | `cleaner@clearline.demo` | Same |

## Do not during recording

| Avoid | Why |
|-------|-----|
| **Read contract again** / force re-extract | Live LLM unless `DEMO_DETERMINISTIC_EXTRACTION=true` on Edge Functions |
| **Generate summary** on dispute detail | Live LLM; seed already has `ai_summary_json` |
| Client portal login | Omitted from this script to save time |
| Contract PDF upload on camera | Meridian contract already uploaded in seed |
| Settings deep dives, onboarding, register | Off-story |

## Sequence

| Timestamp | Beat | Screen | Action | Narration (voiceover, optional) | Visual emphasis |
|-----------|------|--------|--------|--------------------------------|-----------------|
| **0:00–0:12** | Problem | Login → **Dashboard** (owner) | Sign in as owner; land on Dashboard | “Facility teams lose money when contracted work isn’t provable.” | Text overlay on Dashboard: **Problem — no proof of contracted work**; show org name **ClearLine Facility Services** |
| **0:12–0:22** | Contract | **Contracts** → Meridian | Open **Meridian nightly clean**; show version needs review / extracted | “The contract defines what must be done on site.” | Highlight contract title; version status **extracted** / needs review |
| **0:22–0:38** | AI extraction | **Extraction review** | Show two seeded requirements; tap **Approve** | “AI extracts tasks from the PDF; owners approve before they become rules.” | Hold on both requirement lines (~3s); approve animation / success |
| **0:38–0:52** | Service | Log out → cleaner **Today** | Sign in `cleaner@clearline.demo`; open **Meridian Lobby** Today job (after approve) | “Cleaners run today’s jobs from approved requirements.” | Today list → job card **Meridian** → job detail |
| **0:52–1:08** | Evidence | **Task** → **Capture** → **Coverage** | Capture one photo (or confirm existing); open **Coverage** | “Photo evidence links work to each requirement.” | Shutter / confirm; coverage meter or “mandatory complete” |
| **1:08–1:24** | Dispute | Owner → **Disputes** → detail `…4401` | Sign in owner; open dispute; scroll **Timeline** and recorded facts | “When clients dispute scope, we reconstruct recorded facts—not opinions.” | Timeline cards (dock photo, break room exception); **no** “Dispute items are missing” banner |
| **1:24–1:40** | Evidence report | **Report preview** | Scroll sections; tap **Open PDF** (seeded report) | “A neutral evidence report for clients and auditors.” | Preview headings; PDF opens |
| **1:40–1:58** | Subscription | **Paywall** or plan gate | Trigger paywall (e.g. Settings → upgrade, or gated generate on Free plan) | “Pro unlocks unlimited extraction and dispute PDFs via RevenueCat.” | Paywall title, plan copy, subscribe CTA |

## Seed anchors (predictable)

| Beat | ID / record |
|------|-------------|
| Extraction | Version `eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01` (Meridian nightly clean) |
| Today job | Generated on approve (visit weekday **4**); not pre-seeded |
| Dispute | `44444444-4444-4444-8444-444444444401` |
| Report | `22222222-2222-4222-8222-222222222201` (after `upload-assets.sh`) |

## Recording vs judge builds

| Build | Use |
|-------|-----|
| **Recording** | Debug APK; optional `DEMO_SHOW_CREDENTIALS=true` and `DEMO_BYPASS_SUBSCRIPTION=true` in `local.properties` ([demo-mode.md](../development/demo-mode.md)) |
| **Judges** | Release APK; real RevenueCat Test Store; **no** subscription bypass |

If the recording build uses bypass, say in voiceover: “Judges use the Test Store build for purchases.”

## Post-production (optional)

- Burn-in beat titles (Problem, Contract, …) for 2s at each segment start.
- Crop emulator status bar if it shows **DEBUG**.
- Keep total runtime under **2:00**.

## Related

- [golden-path-hackathon.md](golden-path-hackathon.md) — full multi-role path
- [demo-recording-prep.md](demo-recording-prep.md) — pre-flight checklist
- [hackathon-submission-readiness.md](../audits/hackathon-submission-readiness.md) — submit checklist
