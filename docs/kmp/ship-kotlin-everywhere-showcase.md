# Ship Kotlin Everywhere — technical showcase

ContractProof is an Android-first hackathon MVP that **ships shared Kotlin and Compose Multiplatform (CMP)** for the same contract-to-evidence-to-dispute product story on iOS, without duplicating screens or domain rules.

## Executive summary

| Layer | Sharing |
|-------|---------|
| Domain rules & entitlements | 100% `commonMain` |
| Supabase data + offline sync | 100% `commonMain` |
| Compose UI + Navigation3 graph | 100% `commonMain` |
| Shell | Android `MainActivity` vs iOS `MainViewController` + SwiftUI wrapper |
| Platform SDKs | CameraX, RevenueCat, FCM on Android only; UIImagePicker + no-op billing on iOS |

One module ([`composeApp`](../../composeApp/)) compiles to the Android library consumed by [`androidApp`](../../androidApp/) and to the **`ContractProof`** iOS framework consumed by [`iosApp`](../../iosApp/).

## Code reuse metrics

Measured from `composeApp/src` (`.kt` sources, 2026-10-01):

| Source set | Files | Lines (approx.) | Role |
|------------|------:|----------------:|------|
| `commonMain` | 203 | ~21,310 | UI, domain, data, navigation, SQLDelight schema |
| `androidMain` | 20 | ~1,235 | CameraX, RC, PostHog, Sentry, Android file/URI |
| `iosMain` | 17 | ~470 | Darwin Ktor, SQLDelight native, UIKit pickers |

**~94%** of application Kotlin lives in `commonMain`; platform sets are thin `expect`/`actual` boundaries.

## Compose Multiplatform

- **Theme:** [`Theme.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/core/design/Theme.kt) — Material3, ContractProof colors on both targets.
- **Entry:** [`App.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/app/App.kt) → [`AppNavHost.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/app/AppNavHost.kt).
- **Navigation:** AndroidX **Navigation3** with a KMP-safe back stack ([`NavigationSavedState.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/app/NavigationSavedState.kt)) registering every `NavKey` for iOS saved state.
- **DI:** Koin [`appModule`](../../composeApp/src/commonMain/kotlin/com/contractproof/core/AppModule.kt) + [`platformModule()`](../../composeApp/src/commonMain/kotlin/com/contractproof/core/PlatformModule.kt).

### MVP screens (shared)

| Flow | Screens (commonMain) |
|------|----------------------|
| Auth | `LoginScreen`, `CompanySetupScreen`, … |
| Dashboard | `DashboardScreen` |
| Contracts | `ContractsScreen`, `ExtractionReviewScreen` |
| Cleaner service | `TodayScreen`, `JobScreen`, `TaskScreen`, `CaptureScreen`, `CoverageScreen` |
| Dispute | `DisputesScreen`, `DisputeDetailScreen` |
| Report | `ReportPreviewScreen` |

## Platform boundaries (`expect` / `actual`)

| API | Android | iOS | Why split |
|-----|---------|-----|-----------|
| HTTP | OkHttp | Darwin | Ktor engine per platform |
| SQLite | Android driver | Native driver | SQLDelight |
| Evidence camera | CameraX preview | UIImagePicker | OS camera stack |
| Contract PDF pick | `GetContent` | UIDocumentPicker | Storage model |
| Open / share URL | `ACTION_VIEW` / share intent | `openURL` / `UIActivityViewController` | UIKit |
| Subscriptions | RevenueCat | `DisabledSubscriptionService` | MVP scope; Android APK judging |
| Analytics / errors | PostHog / Sentry | No-op | No keys on iOS for hackathon |
| Push | FCM registrar | No-op | Android-only MVP |

Full inventory: [ios-kmp-validation.md](../audits/ios-kmp-validation.md).

## Engineering decisions (genuine KMP, not theater)

1. **Offline-first evidence in common code** — [`SyncCoordinator`](../../composeApp/src/commonMain/kotlin/com/contractproof/data/sync/SyncCoordinator.kt), SQLDelight outbox, and [`EvidenceCompletenessEngine`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/EvidenceCompleteness.kt) run identically on both platforms; only file bytes and upload transport are platform-specific.

2. **Single dispute reconstruction pipeline** — [`DisputeReconstructionRules`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/) and [`EvidenceReportAssemblyRules`](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/EvidenceReportAssemblyRules.kt) feed the same `ReportPreviewScreen` on iOS and Android.

3. **Navigation3 polymorphism for iOS** — Android can use a reflective `rememberNavBackStack(route)` overload; iOS requires an explicit `SerializersModule` for open `NavKey` polymorphism. We registered all routes once in `NavigationSavedState` so **one** `AppNavHost` compiles everywhere.

4. **Intentional SDK gating** — Generated RevenueCat/PostHog/Sentry config is **`androidMain` source only**; iOS compiles without Android artifacts or duplicate feature flags in common code.

5. **Thin iOS shell** — [`MainViewController.kt`](../../composeApp/src/iosMain/kotlin/com/contractproof/app/MainViewController.kt) is ~10 lines; no duplicated ViewModels or Swift UI for MVP flows.

## Consistent UX

- Same route names, titles, and design tokens (`CpButton`, `CpTitleBar`, coverage copy).
- iOS uses photo library / camera pickers instead of inline CameraX preview; user-facing steps match Android (capture → confirm → upload).
- Known iOS deltas: no paywall purchase UI, no push permission flow — documented in [ios-build.md](../development/ios-build.md).

## How to demonstrate

| Audience | Action |
|----------|--------|
| Judges (Android) | Install APK, [golden-path](../qa/golden-path-hackathon.md) |
| Judges (KMP) | Show this doc + `commonMain` package tree + `./gradlew :composeApp:checkIosCompile` |
| macOS | Run [ios-build.md](../development/ios-build.md) simulator smoke |

## What we did not do

- No extra `expect` layers for business logic already expressible in common Kotlin.
- No SwiftUI reimplementation of dashboard/contracts/disputes.
- No artificial “shared” wrappers around one-line platform calls.

## Related

- [ios-kmp-validation.md](../audits/ios-kmp-validation.md)
- [p0-mvp-audit.md](../audits/p0-mvp-audit.md)
