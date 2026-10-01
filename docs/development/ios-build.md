# iOS build and smoke test

ContractProof shares the Compose UI and domain layer via [`composeApp`](../composeApp/). The Swift shell lives in [`iosApp`](../../iosApp/).

## Prerequisites

- macOS with **Xcode 15+** and iOS Simulator
- JDK 17 for Gradle (`JAVA_HOME` or repo [`.jdk17`](../../.jdk17))
- [`local.properties`](../../local.properties) with `SUPABASE_URL` and `SUPABASE_ANON_KEY` (same as Android)
- Supabase ClearLine demo: `supabase db reset` + [`seed.sql`](../../supabase/seed.sql)

## Linux / CI (compile only)

```bash
./gradlew :composeApp:checkIosCompile
```

This compiles `iosSimulatorArm64` and `iosArm64` Kotlin sources; it does not run the simulator.

## macOS — run on Simulator

1. Open **`iosApp/iosApp.xcodeproj`** in Xcode.
2. Set your development team in `iosApp/Configuration/Config.xcconfig` (`TEAM_ID`) if needed for signing.
3. Select an iPhone simulator (e.g. iPhone 15).
4. **Run** (⌘R). The **Compile Kotlin Framework** build phase runs:
   ```bash
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```
5. The app entry point is `MainViewController()` → shared [`App()`](../../composeApp/src/commonMain/kotlin/com/contractproof/app/App.kt).

### Manual golden-path smoke (iOS)

| Step | Action |
|------|--------|
| 1 | Login `owner@clearline.demo` (demo password from [demo-data.md](demo-data.md)) |
| 2 | Dashboard loads ClearLine org |
| 3 | Open **Contracts** — list visible |
| 4 | Log out → login `cleaner@clearline.demo` → **Today** → open job |
| 5 | **Capture** — Take photo or Choose from library |
| 6 | Owner → **Disputes** → open dispute → timeline |
| 7 | **Evidence report** → open PDF URL if seeded |

**iOS MVP limits:** RevenueCat paywall disabled (`DisabledSubscriptionService`); push/analytics no-op. See [ios-kmp-validation.md](../audits/ios-kmp-validation.md).

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Framework not found | Run Gradle embed task from repo root; check `composeApp/build/xcode-frameworks/` |
| Blank Supabase | Regenerate config: `./gradlew :composeApp:generateSupabaseConfig` |
| Camera denied | Allow camera/photos in Simulator Settings → Privacy |

## Related

- [golden-path-hackathon.md](../qa/golden-path-hackathon.md) (Android-focused; same backend)
- [ship-kotlin-everywhere-showcase.md](../kmp/ship-kotlin-everywhere-showcase.md)
