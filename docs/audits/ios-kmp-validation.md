# iOS / KMP validation (prompt 82)

**Date:** 2026-10-01  
**Host:** Linux x86_64 (compile-only); runnable simulator requires macOS + Xcode ([ios-build.md](../development/ios-build.md)).

## Compile verification

| Task | Result |
|------|--------|
| `:composeApp:compileKotlinIosSimulatorArm64` | **PASS** |
| `:composeApp:compileKotlinIosArm64` | **PASS** |
| `:composeApp:checkIosCompile` | Alias for both compile tasks |

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew :composeApp:checkIosCompile
```

**Note:** `iosSimulatorArm64Test` is disabled on non-macOS hosts (expected).

## Fixes applied for iOS compilation

| Issue | Resolution |
|-------|------------|
| Ambiguous `NSData.create` | [`IosFileSupport.kt`](../../composeApp/src/iosMain/kotlin/com/contractproof/core/platform/IosFileSupport.kt) uses `dataWithContentsOfFile` / `dataWithBytes` |
| `Dispatchers.IO` internal on Native | [`App.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/app/App.kt) calls `syncCoordinator.drain()` directly in `LaunchedEffect` |
| `rememberNavBackStack(SplashRoute)` Android-only overload | [`NavigationSavedState.kt`](../../composeApp/src/commonMain/kotlin/com/contractproof/app/NavigationSavedState.kt) + explicit `SavedStateConfiguration` for all `NavKey` subtypes |

## Architecture checklist

| Check | Status | Evidence |
|-------|--------|----------|
| Shared domain | OK | `composeApp/src/commonMain/.../domain/` |
| Shared navigation | OK | Single `AppNavHost`, Navigation3, KMP-safe back stack |
| Shared UI (MVP) | OK | Auth, dashboard, contracts, service, dispute, report screens in `commonMain` |
| Platform `expect`/`actual` | OK | See table below |
| iOS DI boundary | OK | [`PlatformModule.ios.kt`](../../composeApp/src/iosMain/kotlin/com/contractproof/core/PlatformModule.ios.kt) — SQLDelight native, disabled billing, no-op analytics/push |
| Android-only SDKs | OK | RevenueCat, PostHog, Sentry, CameraX only in `androidMain` |

## `expect` / `actual` inventory

| expect | Android | iOS |
|--------|---------|-----|
| `platformModule()` | Koin + RC + camera DB | Native DB + stubs |
| `isDebugBuild()` | Application debug flag | `false` |
| `readLocalFileBytes` | File API | Foundation file I/O |
| `compressEvidencePhoto` | Bitmap compress | File copy (MVP) |
| `EvidencePhotoCapture` | CameraX | UIImagePicker (MVP) |
| `EvidenceCapturePreview` | Coil-style preview | Placeholder text |
| `rememberPdfPickerLauncher` | GetContent | UIDocumentPicker |
| `rememberOpenUrl` | ACTION_VIEW | `UIApplication.openURL` |
| `rememberShareUrl` | Share intent | UIActivityViewController |
| `NotificationPermissionController` | Android 13+ | No-op granted |
| `createContractProofDatabase` | Same schema wrapper | Same |

## Known iOS gaps (acceptable for MVP)

- RevenueCat disabled (`DisabledSubscriptionService`)
- Push notifications no-op
- No PostHog/Sentry on iOS
- Camera UX differs from CameraX (picker-based)

## Regression (Android)

After shared navigation changes, run:

```bash
./gradlew :composeApp:testAndroidHostTest
```
