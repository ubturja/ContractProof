# P0 MVP audit (ContractProof)

**Date:** 2026-10-01  
**Scope:** Hackathon P0 checklist — Android MVP, backend integration, demo data, release artifacts, repository hygiene.  
**Method:** Code and route review, existing audits, seed/golden-path validation, local Gradle verification (see [final-build-report.md](../release/final-build-report.md)).

## Summary

| Status | Count |
|--------|------:|
| Implemented | 22 |
| Partially implemented | 3 |
| Missing | 0 |
| Broken | 0 |

## Requirement matrix

| # | Requirement | Status | Evidence | Notes |
|---|-------------|--------|----------|-------|
| 1 | Android application | **Implemented** | [`androidApp/`](../../androidApp/), [`MainActivity.kt`](../../androidApp/src/main/kotlin/com/contractproof/app/MainActivity.kt), Compose [`composeApp/`](../../composeApp/) | KMP shared UI; Android-first MVP. |
| 2 | Authentication | **Implemented** | [`SupabaseAuthGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/SupabaseAuthGateway.kt), [`LoginScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/auth/LoginScreen.kt), [`SessionController.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/auth/SessionController.kt) | Email/password via Supabase Auth; session restore. |
| 3 | Organization | **Implemented** | [`OrganizationGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/OrganizationGateway.kt), [`CompanySetupScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/auth/CompanySetupScreen.kt) | Create org on onboarding. |
| 4 | Roles | **Implemented** | [`Access.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/Access.kt), [`Role`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/Access.kt), RLS in `supabase/migrations/` | Owner, Manager, Cleaner, Client capabilities. |
| 5 | Clients | **Implemented** | [`ClientsScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/client/ClientsScreen.kt), [`ClientGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/ClientGateway.kt) | CRUD for owner/manager. |
| 6 | Locations | **Implemented** | [`LocationsScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/location/LocationsScreen.kt), [`LocationGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/LocationGateway.kt) | Plan limits via [`Entitlements.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/Entitlements.kt). |
| 7 | Contracts | **Implemented** | [`ContractsScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/contract/ContractsScreen.kt), [`ContractGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/ContractGateway.kt) | Versions, status, linkage to clients/locations. |
| 8 | Contract upload | **Implemented** | [`SupabaseContractGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/SupabaseContractGateway.kt) (`contracts` bucket, `uploadAsFlow`) | PDF upload + signed URL read. |
| 9 | AI extraction | **Implemented** | [`SupabaseExtractionGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/SupabaseExtractionGateway.kt), [`supabase/functions/extract-contract/`](../../supabase/functions/extract-contract/) | JWT + membership; deterministic demo fixture when configured. |
| 10 | Requirement review | **Implemented** | [`ExtractionReviewScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/extraction/ExtractionReviewScreen.kt), [`RequirementsScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/requirement/RequirementsScreen.kt) | Approve extraction → persisted requirements. |
| 11 | Service schedules / jobs | **Implemented** | [`ScheduleGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/ScheduleGateway.kt), [`SupabaseServiceJobGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/SupabaseServiceJobGateway.kt), seed schedules | Today list from assignments + schedule rules. |
| 12 | Cleaner workflow | **Implemented** | [`TodayScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/TodayScreen.kt), [`JobScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/JobScreen.kt), [`TaskScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/TaskScreen.kt), [`CompleteScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/CompleteScreen.kt) | Start → tasks → complete. |
| 13 | Evidence capture | **Implemented** | [`CaptureScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/CaptureScreen.kt), CameraX in `androidApp`, [`SupabaseEvidenceRepository.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/SupabaseEvidenceRepository.kt) | Photo + metadata; `evidence` bucket. |
| 14 | Exceptions | **Implemented** | [`ExceptionScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/ExceptionScreen.kt), [`OfflineFirstExceptionGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/OfflineFirstExceptionGateway.kt) | Queued offline, synced via coordinator. |
| 15 | Evidence completeness | **Implemented** | [`EvidenceCompletenessEngine.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/EvidenceCompleteness.kt), [`CoverageScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/CoverageScreen.kt) | Pending upload ≠ missing evidence (domain rules). |
| 16 | Offline sync | **Implemented** | [`SyncCoordinator.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/sync/SyncCoordinator.kt), SQLDelight outbox, [`App.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/app/App.kt) | Evidence/exception retry; offline banners in controllers. |
| 17 | Service history | **Implemented** | [`ServiceScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/service/ServiceScreen.kt), [`ClientHomeScreen`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/clientportal/ClientPortalScreens.kt) | Completed jobs for staff and client portal. |
| 18 | Disputes | **Implemented** | [`DisputesScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/dispute/DisputesScreen.kt), [`SupabaseDisputeGateway.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/SupabaseDisputeGateway.kt) | Client file + owner/manager review. |
| 19 | Dispute reconstruction | **Implemented** | [`DisputeDetailScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/dispute/DisputeDetailScreen.kt), [`DisputeReconstructionBundle`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/Dispute.kt), [`ServiceRecordAssemblyRules.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/ServiceRecordAssemblyRules.kt) | Timeline + recorded facts; seed `dispute_items`. |
| 20 | PDF report | **Implemented** | [`ReportPreviewScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/report/ReportPreviewScreen.kt), [`generate-dispute-report`](../../supabase/functions/generate-dispute-report/) | Preview + signed URL; seeded report for demo. |
| 21 | RevenueCat | **Partially implemented** | [`RevenueCatInitializer.kt`](../../composeApp/src/androidMain/kotlin/com/contractproof/subscription/RevenueCatInitializer.kt), [`PaywallScreen.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/feature/subscription/PaywallScreen.kt), [`Entitlements.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/Entitlements.kt) | Works when API key configured; debug demo bypass only. No server-side entitlement gate on Edge Functions (accepted MVP risk per [security-mvp.md](security-mvp.md)). |
| 22 | Demo data | **Implemented** | [`supabase/seed.sql`](../../supabase/seed.sql), [`docs/development/demo-data.md`](../development/demo-data.md), [`upload-assets.sh`](../../supabase/seed/upload-assets.sh) | ClearLine fictional org; golden path documented. |
| 23 | APK build | **Implemented** | [`scripts/assemble-release-apk.sh`](../../scripts/assemble-release-apk.sh), [`androidApp/build.gradle.kts`](../../androidApp/build.gradle.kts) | Universal APK to `dist/`; signing documented. |
| 24 | README | **Implemented** | [`README.md`](../../README.md) | Setup, architecture, screenshots section. |
| 25 | Repository | **Partially implemented** | LICENSE, CONTRIBUTING, SECURITY, `.gitignore` | Public hygiene in place; see defects below. |

## Prioritized defects

### Critical

_None identified in this audit._ Runtime crashes, tenant isolation, and auth bypass were addressed in prior security work ([security-mvp.md](security-mvp.md)).

### High

| ID | Area | Description | Recommendation |
|----|------|-------------|----------------|
| H1 | Docs / CI | [`CONTRIBUTING.md`](../../CONTRIBUTING.md) references non-existent `:composeApp:testDebugUnitTest`. Actual host task is `:composeApp:testAndroidHostTest`. | Fix CONTRIBUTING and README test commands. |
| H2 | Marketing assets | Hackathon screenshots in `docs/assets/screenshots/` are **reference composites** until `scripts/capture-screenshots.sh` runs on a device. | Replace with device captures before store submission; documented in [screenshots.md](../qa/screenshots.md). |

### Medium

| ID | Area | Description |
|----|------|-------------|
| M1 | Subscriptions | Paid features enforced client-side only; Edge Functions do not check RevenueCat. |
| M2 | Instrumented QA | `connectedDebugAndroidTest` requires emulator/device; not run in default CI on Linux agents. |
| M3 | iOS | KMP `ios` target present; simulator tests require macOS (expected). |

### Low

| ID | Area | Description |
|----|------|-------------|
| L1 | Launcher | Was missing before prompt 77; resolved with adaptive icon + manifest. |

## Automated test coverage (spot check)

- Unit/host: `composeApp/src/commonTest/` — domain rules, `SyncCoordinator`, integration workflows.
- Android instrumented: [`GoldenPathSmokeTest.kt`](../../androidApp/src/androidTest/kotlin/com/contractproof/app/GoldenPathSmokeTest.kt), capture/coverage tests.
- Supabase: `supabase/tests/rls.sql`, `workflows.sql` (manual/CI against Supabase).

## Related audits

- [security-mvp.md](security-mvp.md)
- [unit-test-coverage.md](unit-test-coverage.md)
- [performance-mvp.md](performance-mvp.md)
- [android-mvp-checklist.md](../qa/android-mvp-checklist.md)
