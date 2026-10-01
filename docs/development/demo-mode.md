# Demo mode (safe, no security bypass)

Demo mode uses **real sign-in**, **RLS**, and rows in [`supabase/seed.sql`](../supabase/seed.sql) marked `is_demo = true`. The app does not inject SQL, forge evidence, or skip authentication.

## 1. Reset database with seed

```bash
supabase db reset
```

## 2. Create Auth users

Create four users in Supabase Auth whose **UUIDs** match [demo-data.md](demo-data.md).

Use the shared demo password documented in demo-data.md.

## 3. Upload demo storage files

```bash
./supabase/seed/upload-assets.sh
```

Uploads contract PDFs, Northstar evidence JPEG, and optional pre-generated dispute report PDF.

## 4. Edge function: deterministic re-extract (optional)

On the Supabase project, set Edge Function secret or env:

```bash
DEMO_DETERMINISTIC_EXTRACTION=true
```

When enabled, `extract-contract` returns the Meridian fixture for version `eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01` instead of calling Gemini/Groq. **Primary demo path** uses seed `extraction` JSON already on that version—no LLM required if you do not tap “Read contract again”.

## 5. Show credential hints in debug APK

In `local.properties`:

```properties
DEMO_SHOW_CREDENTIALS=true
```

Rebuild debug. The login screen lists demo **emails** only.

## 6. Demo walkthrough

See [golden-path-hackathon.md](../qa/golden-path-hackathon.md) for the full role-by-role script.

| Role | Start screen | Story |
|------|----------------|--------|
| Owner | Contracts → Meridian nightly clean | Review extracted requirements → approve |
| Cleaner | Today | Meridian job after approve — capture tasks |
| Client | Client home | Northstar disputed service + open dispute |
| Owner/Manager | Disputes | Open dispute `...4401` → evidence report |

Optional: `DEMO_BYPASS_SUBSCRIPTION=true` in `local.properties` for **debug** subscription bypass (recording only). Judge builds should leave this false.

## Do not during primary demo

- Tap **Read contract again** / force re-extract (live LLM unless deterministic env is set).
- Tap **Generate summary** on dispute detail if no cached summary (live LLM). Seeded dispute includes `ai_summary_json`.
