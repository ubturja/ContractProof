# Final golden flow (canonical judge script)

**Product message:** *Contracts say what must happen. ContractProof proves what happened.*

This is the **authoritative** demo sequence for judges, video, and QA. Shorter variants: [hackathon-demo-video-script.md](hackathon-demo-video-script.md), [golden-path-hackathon.md](golden-path-hackathon.md).

**Tenant:** ClearLine Facility Services (fictional). Reset: `supabase db reset`, auth users per [demo-data.md](../development/demo-data.md), `./supabase/seed/upload-assets.sh`.

## Sequence

| Step | Role | What happens | Screen / route | Seed / notes |
|------|------|----------------|----------------|--------------|
| 1 | Owner | Opens ContractProof | Login → Dashboard | `owner@clearline.demo` |
| 2 | Owner | Selects **Meridian Office Tower** context | Clients / Locations / Contracts filter | Client `bbbb…bb01`, location **Meridian Lobby** |
| 3 | Owner | Contract is available | **Contracts** → **Meridian nightly clean** | Contract `dddd…dd01` |
| 4 | Owner | Sees extracted cleaning requirements (no live LLM if seeded) | Contract detail → **Extraction review** | Version `eeee…ee01`, status `extracted` |
| 5 | Owner | Reviews and **approves** requirements | Extraction review → Approve | Creates requirements + weekly visit schedule |
| 6 | Cleaner | Opens today’s service | **Today** | `cleaner@clearline.demo` — job appears after approve / schedule |
| 7 | Cleaner | Sees **contractual** requirements | Job → **Tasks** | From approved Meridian contract |
| 8 | Cleaner | Starts service | Job → Start | |
| 9 | Cleaner | Completes tasks | Task list | |
| 10 | Cleaner | Captures required evidence | **Capture** (photo) | |
| 11 | Owner/Cleaner | **Evidence coverage** calculated | **Coverage** | Pending upload ≠ missing proof |
| 12 | Cleaner | Completes service | **Complete** | When mandatory evidence satisfied |
| 13 | Client | Submits a dispute | Client portal | `client@clearline.demo` — Northstar disputed job `…9902` (pre-seeded) **or** file new dispute per app rules |
| 14 | Owner | Opens dispute | **Disputes** → detail | `44444444-4444-4444-8444-444444444401` |
| 15 | Owner | **Reconstruction** — factual timeline | Dispute detail **Timeline** | Seeded items + evidence + exception |
| 16 | Owner | **PDF evidence report** | **Report preview** → Open PDF | Report `22222222-2222-4222-8222-222222222201` |
| 17 | Any | **Subscription** where relevant | **Paywall** / plan gate | RevenueCat Test Store on judge APK; extraction/report gates on Free |

## Do not (breaks determinism or scope)

| Action | Why |
|--------|-----|
| **Read contract again** / force re-extract | Live LLM unless `DEMO_DETERMINISTIC_EXTRACTION=true` |
| **Generate summary** on empty dispute | Live LLM; seed includes `ai_summary_json` |
| Skip owner **approve** before cleaner Today | Meridian job/tasks depend on approved requirements / schedule |
| Onboarding, register, unrelated settings | Off-story for judges |

## Meridian anchors

| Item | ID |
|------|-----|
| Client | Meridian Office Tower `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb01` |
| Location | Meridian Lobby `cccccccc-cccc-4ccc-8ccc-cccccccccc01` |
| Contract | Meridian nightly clean `dddddddd-dddd-4ddd-8ddd-dddddddddd01` |
| Extraction version | `eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01` |
| Open dispute | `44444444-4444-4444-8444-444444444401` |

## What judges should understand

One thread: **contract rules → field proof → dispute facts → exportable report → paid features for scale.**

## Related

- [mvp-freeze.md](../hackathon/mvp-freeze.md)
- [demo-recording-prep.md](demo-recording-prep.md)
