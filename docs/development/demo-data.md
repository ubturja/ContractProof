# ClearLine demo data (fictional)

All names and contacts are invented for hackathon demos. Do not use real businesses.

## Shared demo password

After seeding, create Supabase Auth users with this password (example only):

`ClearLine-Demo-2026!`

Use the Supabase dashboard or CLI (see [demo-mode.md](demo-mode.md)).

## Organization

| Field | Value |
|-------|--------|
| Name | ClearLine Facility Services |
| ID | `11111111-1111-4111-8111-111111111101` |

## Accounts

| Role | Email | User ID |
|------|--------|---------|
| Owner | owner@clearline.demo | `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01` |
| Manager | manager@clearline.demo | `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa02` |
| Cleaner | cleaner@clearline.demo | `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03` |
| Client | client@clearline.demo | `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa04` |

Client membership is linked to **Northstar Logistics** (`bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02`) so the seeded open dispute and completed service are visible under client RLS.

## Clients

- Meridian Office Tower — `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb01`
- Northstar Logistics — `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02`
- Westbridge Medical Offices — `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb03`

## Golden-path records

| Scenario | ID | Notes |
|----------|-----|--------|
| Owner extraction (awaiting approval) | Contract `dddddddd-dddd-4ddd-8ddd-dddddddddd01` (Meridian nightly clean), version `eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01` | Status `extracted`, deterministic JSON in seed |
| Cleaner Today | *(after owner approves `…ee01`)* | App generates Meridian job for visit weekday **4** (Thursday) on approve — see [final-golden-flow.md](../qa/final-golden-flow.md) |
| Westbridge (secondary) | Contract `dddddddd-dddd-4ddd-8ddd-dddddddddd03`, version `eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03` | Status `approved` (no pending extraction) |
| Disputed service | Job `99999999-9999-4999-8999-999999999902` | Northstar, service date **2026-10-01** |
| Open dispute | `44444444-4444-4444-8444-444444444401` | Pre-seeded items + AI summary JSON |
| Dispute item (dock photo) | `33333333-3333-4333-8333-333333333301` | Outcome `satisfied` |
| Dispute item (break room) | `33333333-3333-4333-8333-333333333302` | Outcome `exception` |
| Pre-generated report | `22222222-2222-4222-8222-222222222201` | PDF in `reports` bucket after upload script |

## Storage upload

From repo root after `supabase db reset`:

```bash
./supabase/seed/upload-assets.sh
```

Assets live under [`supabase/seed/assets/`](../supabase/seed/assets/). Legacy alias: `meridian-dock-evidence.jpg` is the same bytes as `northstar-dock-evidence.jpg`.

Object paths must match [`supabase/seed.sql`](../supabase/seed.sql).
