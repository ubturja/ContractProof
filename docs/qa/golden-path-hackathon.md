# Hackathon golden path (ContractProof)

Multi-role script for a ~2 minute demo. **Canonical sequence:** [final-golden-flow.md](final-golden-flow.md). Same routes as [navigation.md](../architecture/navigation.md#golden-demo-path). Prerequisites: [demo-mode.md](../development/demo-mode.md), [demo-data.md](../development/demo-data.md), [ux-states.md](../architecture/ux-states.md).

## Prerequisites

1. `supabase db reset`
2. Auth users (fixed UUIDs) with shared password from demo-data
3. `./supabase/seed/upload-assets.sh`
4. Debug APK with Supabase config; optional `DEMO_SHOW_CREDENTIALS=true`, `DEMO_BYPASS_SUBSCRIPTION=true` for **recording only**

## Do not during primary demo

| Action | Why |
|--------|-----|
| **Read contract again** / force re-extract | Calls live LLM unless `DEMO_DETERMINISTIC_EXTRACTION=true` on Edge Functions |
| **Generate summary** on dispute detail (when empty) | Calls `summarize-dispute` LLM; seeded dispute already has `ai_summary_json` |

## Scripted path

| Step | Role | Screen / route | Seed or action | Expected result |
|------|------|----------------|----------------|-----------------|
| 1 | Owner | Login | `owner@clearline.demo` | Dashboard |
| 2 | Owner | Contracts | **Meridian nightly clean** | Version shows **extracted** / needs review |
| 3 | Owner | ExtractionReview | Version `eeee...ee01` | Two lobby tasks from seed JSON; helper copy about approve |
| 4 | Owner | ExtractionReview | Approve | Version approved; requirements + visit schedule created (no live LLM if extraction already seeded) |
| 5 | Cleaner | Login → Today | `cleaner@clearline.demo` | **Meridian Lobby** job after approve (visit weekday **4** / Thursday) |
| 6 | Cleaner | Job → Task → Capture | Meridian Today job | Capture photo for mandatory task; pending upload OK |
| 7 | Cleaner | Coverage / Job | Same job | Coverage increases; finish when mandatory satisfied |
| 8 | Client | Login → Client home | `client@clearline.demo` | Northstar completed/disputed service visible |
| 9 | Client | Service record / dispute | Job `...9902` | Timeline shows dock evidence + break room exception |
| 10 | Owner | Disputes → Detail | Dispute `4444...4401` | Timeline populated; cached AI summary visible |
| 11 | Owner/Manager | ReportPreview | Same dispute | Preview sections match job; **Open PDF** works if report seeded + uploaded |
| 12 | Owner | Paywall (optional) | Settings or gated generate | Free plan shows gate; recording build may use bypass |

## Paywall / subscription

- **Judge APK:** real RevenueCat test store; dispute PDF generate may open Paywall on Free.
- **Recording:** `DEMO_BYPASS_SUBSCRIPTION=true` in `local.properties` (debug only).

## Validation checklist

After each seed reset + storage upload:

- [ ] Meridian extraction review shows same two requirements every time
- [ ] Owner **approve** completed before cleaner segment; Today lists Meridian job on **Thursday** (or matching visit weekday in seed)
- [ ] Client sees Northstar dispute (client `client_id` = Northstar in seed)
- [ ] Dispute detail timeline non-empty; no “Dispute items are missing” banner
- [ ] Report preview loads; optional instant PDF from seeded `reports` row

## Related

- [final-golden-flow.md](final-golden-flow.md) — canonical judge script
- [hackathon-demo-video-script.md](hackathon-demo-video-script.md) — under-2-minute recording script (problem → subscription)
- [demo-recording-prep.md](demo-recording-prep.md) — pre-flight checklist before filming
- [hackathon-submission-readiness.md](../audits/hackathon-submission-readiness.md) — PASS / FAIL / NEEDS ATTENTION submit audit
- [android-mvp-checklist.md](android-mvp-checklist.md) — broader manual QA
- [unit-test-coverage.md](../audits/unit-test-coverage.md) — automated rules coverage
