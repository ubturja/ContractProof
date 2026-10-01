# Android MVP performance audit (Prompt 61)

**Date:** 2026-10-01  
**Scope:** Cold start, navigation refresh patterns, SQLDelight, evidence preview/upload, memory, recomposition, network.

## Changes made

| Area | Issue | Fix |
|------|--------|-----|
| **Startup** | PostHog, Sentry, and RevenueCat initialized synchronously in `onCreate` before `setContent`. | `initPlatformContext` only stores `applicationContext`. SDK configuration runs once via `ensureAndroidSdks()` on first Koin platform module resolution (after first frame). |
| **Sync on launch** | Outbox drain on main dispatcher. | `App.kt` runs `syncCoordinator.drain()` inside `withContext(Dispatchers.IO)`. |
| **Client noise** | `println` on Supabase client creation. | Removed from `SupabaseClientFactory.kt`. |
| **Navigation** | `ContractsRoute` used four separate `LaunchedEffect(Unit)` calls for refresh. | Merged list refreshes into one effect; health summaries still keyed on `contractsState.items`. |
| **Image preview** | Full-resolution `BitmapFactory.decodeFile` on Task screen. | `decodePreviewBitmap` caps edge at 1024px with sampling; bitmap recycled on dispose. |
| **Upload** | `uploadPhotoEvidence` re-read staging file after compress. | In-memory `photoStagingBytes` map keyed by org + requirement; removed after upload; disk read only on retry/cold path. |

## Left unchanged (intentional)

- **Database:** SQLDelight queries and sync queue indexes are adequate for MVP; no query rewrites without profiling data.
- **Compression:** Existing 2048px / JPEG 85 pipeline kept as-is.
- **NavHost `collectAsState`:** Root-level state collection is normal for this architecture; no broad refactor.
- **Parallel dashboard/contract network refresh:** Intentional; offline-first caches limit repeat cost.
- **Recomposition:** No Compose layout-inspector pass; no unstable-lambda churn identified worth fixing.

## Verification

- Run `./gradlew :composeApp:testAndroidHostTest` when a JDK 17+ **compiler** toolchain is available (project targets JVM 17).
- Manual: cold start to login, open Task with a large capture, single photo upload.

## Residual risks

- SDK init still runs on the thread that first resolves Koin platform bindings (typically main during composition), but no longer blocks `MainActivity.onCreate` before `setContent`.
- Staging bytes live in process memory until upload completes; acceptable for single concurrent capture per requirement.
