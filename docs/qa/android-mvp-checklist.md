# Android MVP manual QA checklist

Prerequisites: debug APK, Supabase project with [`seed.sql`](../../supabase/seed.sql) and auth users per [demo-data.md](../development/demo-data.md). Optional: `DEMO_SHOW_CREDENTIALS=true`, `DEMO_BYPASS_SUBSCRIPTION=true` (debug only). See [ux-states.md](../architecture/ux-states.md), [navigation.md](../architecture/navigation.md), [demo-mode.md](../development/demo-mode.md).

| ID | Area | Steps | Expected result | Pass / Fail |
|----|------|--------|-----------------|-------------|
| QA-001 | Installation | Install debug APK on a physical device or emulator (API 26+). | App icon launches without crash. | |
| QA-002 | First launch | Cold start with no session. | Splash → onboarding carousel → login (or login if onboarding completed). | |
| QA-003 | Register | Register new owner email + password. | Account created; navigates to company setup when no org. | |
| QA-004 | Login | Sign in with seeded `owner@clearline.demo` (password in demo-data doc). | Owner lands on dashboard with ClearLine org. | |
| QA-005 | Demo hints | Build with `DEMO_SHOW_CREDENTIALS=true`; open login. | Collapsible demo account emails shown; password not in APK. | |
| QA-006 | Company setup | New owner: enter company + display name, submit. | Dashboard; org name visible in settings. | |
| QA-007 | Sign out / in | Settings → sign out; sign in as cleaner. | Cleaner lands on Today (not dashboard). | |
| QA-008 | Dashboard | Owner: open dashboard. | Coverage alerts, disputes, today’s jobs sections load or empty states. | |
| QA-009 | Locations | Owner: locations list → add/edit location. | Location saves; appears in list. | |
| QA-010 | Clients | Owner: create client under org. | Client available when creating contract/location. | |
| QA-011 | Contract upload | Owner: contracts → upload PDF or manual draft. | Draft contract created; extraction review reachable. | |
| QA-012 | Extraction review | Edit requirements; approve version. | Approved version; requirements attached. | |
| QA-013 | Service jobs | Owner: view today’s job from dashboard or schedule. | Job detail opens with requirement list. | |
| QA-014 | Cleaner Today | Cleaner: Today list. | Meridian job visible **after** owner approves `…ee01` (Thursday visit). | |
| QA-015 | Start service | Cleaner: open job → start service. | Status in progress; tasks actionable. | |
| QA-016 | Capture photo | Task → capture → confirm photo. | Local preview; sync state pending/uploading. | |
| QA-017 | Exception | Mandatory task → report exception with reason. | Exception recorded; requirement shows exception state. | |
| QA-018 | Coverage | Open coverage from job. | Percent updates; missing items listed. | |
| QA-019 | Complete job | Satisfy mandatory items (evidence or exception); finish. | Job completed when rules allow; pending upload does not block finish. | |
| QA-020 | Offline job | Airplane mode on job screen; reopen job. | Offline banner; cached job still readable. | |
| QA-021 | Upload retry | Force failed upload (offline during upload); retry. | Failed state with retry; succeeds when online. | |
| QA-022 | Service record | Owner: service history → completed job. | Read-only timeline with evidence/exception. | |
| QA-023 | Dispute create | Owner: disputes → create for completed job. | Dispute saved; appears in list. | |
| QA-024 | Dispute detail | Open seeded open dispute (Northstar). | Neutral timeline; no “winner” copy. | |
| QA-025 | Report preview | Manager/owner: report preview for dispute (Pro). | Assembled report sections match job data. | |
| QA-026 | Paywall gate | Free plan: tap dispute PDF generate. | Paywall opens; generate disabled. | |
| QA-027 | Client home | Sign in as `client@clearline.demo`. | Client home with completed/disputed services. | |
| QA-028 | Client dispute | Client: open service → file dispute flow. | Confirmation; dispute visible to owner. | |
| QA-029 | Settings | Open settings from shell. | Account, org (owner), sync status, subscription entry. | |
| QA-030 | Notifications | Grant notification permission; trigger push (if configured). | Tap routes to job/dispute per routing rules. | |
| QA-031 | Paywall purchase | Paywall → purchase (RevenueCat test store). | Entitlement updates; gated actions unlock. | |
| QA-032 | Restore purchases | Paywall → restore purchases. | Prior entitlement restored or clear error. | |
