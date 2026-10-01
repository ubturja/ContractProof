# Hackathon submission readiness audit

**MVP frozen (post–Prompt 89):** Scope and judge story are locked in [mvp-freeze.md](../hackathon/mvp-freeze.md) and [final-golden-flow.md](../qa/final-golden-flow.md). This audit is not re-run automatically after the freeze.

**Date:** 2026-10-01  
**Scope:** Prompt 87 — final checklist for ContractProof hackathon submission.  
**Method:** Repository inspection, prior build/audit docs, local artifacts. **No automatic fixes applied.**

Status legend: **PASS** | **FAIL** | **NEEDS ATTENTION**

---

## Project

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Android app works | **PASS** | [p0-mvp-audit.md](p0-mvp-audit.md) — Android MVP implemented; golden path documented |
| APK downloadable | **NEEDS ATTENTION** | Universal APK built locally (`dist/ContractProof-1.0.0-1-universal.apk` per [final-build-report.md](../release/final-build-report.md)); GitHub Release publish is manual ([publish-github-release.md](../release/publish-github-release.md)); `gh` CLI not available in audit environment to confirm remote asset |
| APK installs | **NEEDS ATTENTION** | Install steps in [apk-verification.md](../release/apk-verification.md); last build host had no `adb` — judge/device install not re-verified here |
| Repository public | **NEEDS ATTENTION** | Remote documented as `https://github.com/ubturja/ContractProof` in release docs; visibility not verified without `gh repo view` |
| Repository contains license | **PASS** | [LICENSE](../../LICENSE) (MIT) at repo root |
| README complete | **NEEDS ATTENTION** | [README.md](../../README.md) covers setup, architecture, screenshots, KMP; **Demo video** section still placeholder link; screenshots are reference composites per [screenshots.md](../qa/screenshots.md) |

---

## Demo

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Video under two minutes | **FAIL** | No hosted demo URL in README; script ready at [hackathon-demo-video-script.md](../qa/hackathon-demo-video-script.md) (~1:58 target) — **record and link** |
| Demo shows actual working app | **PASS** | Script follows seeded ClearLine path; [golden-path-hackathon.md](../qa/golden-path-hackathon.md) + [demo-recording-prep.md](../qa/demo-recording-prep.md) |
| No prohibited copyrighted material | **PASS** | Fictional ClearLine data; original icon ([app-icon.md](../design/app-icon.md)); no third-party assets in marketing composites |
| English materials | **PASS** | README, docs, and UI copy in English |

---

## Product

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Clear concept | **PASS** | README problem/solution — contract → evidence → dispute |
| Clear problem | **PASS** | README + video script problem beat |
| Clear differentiation | **PASS** | README “Why it is different” — approved contract versions, pending upload ≠ missing proof |
| Clear monetization | **PASS** | README RevenueCat; Free/Pro gating in [Entitlements](../../composeApp/src/commonMain/kotlin/com/contractproof/domain/Entitlements.kt) |
| RevenueCat purchase exists | **NEEDS ATTENTION** | Android: [RevenueCatInitializer](../../composeApp/src/androidMain/kotlin/com/contractproof/subscription/RevenueCatInitializer.kt) requires API key in build config; judges need Test Store + key in release build; iOS uses `DisabledSubscriptionService` (Android-first judging) |

---

## Technical

| Criterion | Status | Evidence |
|-----------|--------|----------|
| KMP / CMP used meaningfully | **PASS** | ~94% Kotlin in `commonMain`; [ship-kotlin-everywhere-showcase.md](../kmp/ship-kotlin-everywhere-showcase.md); `./gradlew :composeApp:checkIosCompile` |
| Backend secure | **PASS** | [security-mvp.md](security-mvp.md) — RLS, private storage, JWT on edge functions |
| AI controlled | **PASS** | Human approval for extraction; seed + optional `DEMO_DETERMINISTIC_EXTRACTION`; no auto-apply to requirements |
| Offline workflow works | **PASS** | SQLDelight + [SyncCoordinator](../../composeApp/src/commonMain/kotlin/com/contractproof/data/sync/SyncCoordinator.kt); README offline section |
| Tests pass | **PASS** | [final-build-report.md](../release/final-build-report.md) — `:composeApp:testAndroidHostTest` PASS; re-run via `./scripts/verify-demo-recording.sh` |

---

## Summary counts

| Status | Count |
|--------|------:|
| PASS | 14 |
| NEEDS ATTENTION | 7 |
| FAIL | 1 |

---

## Submit blockers (human actions)

Ordered by impact for hackathon judges:

1. **FAIL — Demo video:** Record per [hackathon-demo-video-script.md](../qa/hackathon-demo-video-script.md); upload (YouTube/Loom); add URL to README § Demo video.
2. **NEEDS ATTENTION — APK downloadable:** Create GitHub Release `v1.0.0` with universal APK ([publish-github-release.md](../release/publish-github-release.md)); update README download link and SHA-256 from [RELEASE_METADATA.md](../release/RELEASE_METADATA.md).
3. **NEEDS ATTENTION — APK installs:** Smoke-install release APK on one physical device or emulator; document result in release notes if needed.
4. **NEEDS ATTENTION — Repository public:** Confirm GitHub repo is public and accessible to judges.
5. **NEEDS ATTENTION — README:** Replace screenshot reference frames with device captures when possible ([screenshots.md](../qa/screenshots.md)).
6. **NEEDS ATTENTION — RevenueCat:** Confirm judge build includes valid Test Store key; document test account steps in README or release notes.

---

## Pre-submit verification commands

```bash
./scripts/verify-demo-recording.sh
./gradlew :composeApp:checkIosCompile
CONTRACTPROOF_USE_DEBUG_SIGNING=true ./scripts/assemble-release-apk.sh
./scripts/release-metadata.sh
```

Do **not** commit APK binaries or `local.properties`.

---

## Related

- [p0-mvp-audit.md](p0-mvp-audit.md)
- [hackathon-demo-video-script.md](../qa/hackathon-demo-video-script.md)
- [demo-recording-prep.md](../qa/demo-recording-prep.md)
