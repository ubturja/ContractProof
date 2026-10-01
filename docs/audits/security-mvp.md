# ContractProof security audit (Prompt 62)

**Date:** 2026-10-01  
**Scope:** RLS, authZ, storage, secrets, edge functions, local data, logs, subscriptions.

## Secrets and mobile client

- **Supabase anon key and URL** are generated at build time into `build/generated/` via `GenerateSupabaseConfigTask`; not committed. `local.properties` and `.env` are gitignored.
- **No service role key** in the mobile app; service role is used only in Supabase Edge Functions via environment variables.
- **PostHog / RevenueCat / Sentry** keys follow the same generated-config pattern.
- **Expectation:** Anon key in the APK is normal; tenant isolation relies on **RLS** and user JWTs, not key secrecy.

## RLS and organization isolation

- Tenant boundary: `private.current_membership()` and policies in `20260930170000_row_level_security.sql`.
- Client portal read/insert rules: `20261001100000_client_portal_rls.sql` (completed jobs, own disputes, client-scoped locations).
- Automated checks: `supabase/tests/rls.sql` and `supabase/tests/storage.sql` (run against a local or CI Supabase instance).

## Storage

- Buckets `contracts`, `evidence`, `reports`, `dispute-attachments` are **private** (`public = false`) with org- and role-scoped policies (e.g. `20261001070000_evidence_storage_policies.sql`).

## App authorization

- Role capabilities: `Access` + `AppNavHost` guards and `NotificationRoutingRules` (clients do not receive owner dispute/report deep links).

## Edge / AI functions

- `extract-contract`, `summarize-dispute`, and `generate-dispute-report` validate Bearer JWT and membership role in function code.
- **`supabase/config.toml`:** `verify_jwt = true` for all four functions including `notify-dispute-received`.

## Critical fix: `notify-dispute-received`

**Before:** Unauthenticated POST could trigger FCM to organization owners for arbitrary `dispute_id` / `organization_id`.

**After:** `auth.ts` requires a signed-in **client** whose membership matches `organization_id`, and a dispute row that matches `client_id` and `recorded_by = auth.uid()`. Unauthorized requests return 401/403. Minimal Deno test in `auth_test.ts`.

## push_device_tokens

- RLS limits users to their own token rows; edge function uses service client to read owner tokens after auth — acceptable.

## Subscription

- **Demo bypass:** `DemoBypassSubscriptionService` only applies when `RevenueCatLocalConfig.demoBypassSubscription` is true **and** `isDebugBuild()` (Android `BuildConfig.DEBUG`; iOS always false).
- **Residual:** Entitlements are enforced in app/domain and RevenueCat on device; there is no server-side subscription gate for all APIs in MVP.

## Logs

- No passwords or tokens logged in new code paths. Analytics/observability sanitizers redact sensitive keys. Removed Supabase host `println` from client init.

## Verification

- `./gradlew :composeApp:testAndroidHostTest`
- `deno test` in `supabase/functions/notify-dispute-received/` (when Deno available)
- `psql` / Supabase CLI: run `supabase/tests/rls.sql` and `storage.sql`

## Residual risks (accepted for MVP)

- Anon key extractable from APK; mitigate with RLS and short-lived user sessions.
- Client-side subscription trust; paid features should gain server checks before scaling.
- `notify-dispute-received` still uses legacy FCM HTTP API with server key in function env (operational secret, not in repo).
