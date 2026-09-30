# ContractProof dependency decisions

This document chooses the technologies for the Android-first MVP. It follows [mvp.md](mvp.md). It does not add libraries.

Priority labels:

- **P0:** required for the Android golden path or hackathon eligibility. Add it only when the feature prompt arrives.
- **P1:** needed after the golden path for a complete MVP.
- **P2:** a named hook. No SDK until a later prompt asks for it.

Versions already pinned in `gradle/libs.versions.toml` stay as they are:

- Kotlin 2.4.20
- Compose Multiplatform 1.12.1
- Android Gradle Plugin 9.1.1
- Activity Compose 1.13.0

Every library that is not in the catalog yet is named here. Its version is chosen when it is introduced, and that version must stay compatible with the pins above.

## Kotlin

- **Purpose:** the language for the mobile app.
- **Why:** one language covers shared business rules, Compose UI, and the Android entry point.
- **Priority:** P0. Already present.
- **Where:** `composeApp` and `androidApp`.

## Kotlin Multiplatform

- **Purpose:** share domain, data contracts, sync rules, and UI between Android now and iOS later.
- **Why:** the product needs one evidence workflow, not a second app. The iOS target is added later in the same module.
- **Priority:** P0. Already present.
- **Where:** `composeApp` is the shared module. It is an Android library target today. `androidApp` is not shared.

## Compose Multiplatform

- **Purpose:** the shared UI toolkit.
- **Why:** owner and cleaner screens are the product. Material 3 keeps those screens consistent without a second UI stack.
- **Priority:** P0. Already present.
- **Where:** UI lives in `composeApp` `commonMain`. `androidApp` hosts `MainActivity` and calls `App()`.

## Navigation

- **Purpose:** move between auth, owner, cleaner, dispute, and subscription screens.
- **Why:** JetBrains multiplatform Navigation Compose is the navigation library that matches Compose Multiplatform. Routes stay type-safe. Voyager, Decompose, and a hand-rolled back stack are not used.
- **Priority:** P0. Not added yet.
- **Where:** `composeApp` `commonMain`, package `com.contractproof.app`.

## Koin

- **Purpose:** construct repositories, use cases, and the sync worker.
- **Why:** the graph is small. Koin can live in `commonMain` without Android-only code generation. Dagger and Hilt are not used.
- **Priority:** P0. Not added yet.
- **Where:** `koin-core` and `koin-compose` in `commonMain`, package `com.contractproof.core`.

## Coroutines and Flow

- **Purpose:** background work and screen state.
- **Why:** they are the Kotlin model for async work. ViewModels expose `StateFlow`. RxJava and a second async library are not used.
- **Priority:** P0. `kotlinx-coroutines-core` is added in `commonMain` when the first use case needs it. Compose may already bring coroutines transitively; the catalog still names the dependency when feature code starts.
- **Where:** `composeApp` `commonMain`. Domain use cases and the data layer both use them. UI collects `StateFlow`.

## SQLDelight

- **Purpose:** the on-device database for cached jobs, requirements, evidence metadata, exceptions, and the upload outbox.
- **Why:** cleaners work with poor connectivity. The UI reads the local database. Photo bytes stay as file paths, not blobs.
- **Priority:** P0. Not added yet.
- **Where:** the SQLDelight plugin and runtime live in `composeApp`. The Android driver lives in `androidMain`. Schema and queries live in the data layer. The iOS driver is a later `iosMain` actual.

## Supabase

- **Purpose:** auth, Postgres API, private file storage, and Edge Functions.
- **Why:** one hosted backend covers tenancy, files, and server-side AI. The app uses the anon key. The service-role key stays on the server.
- **Priority:** P0. Not added yet.
- **Where:** `supabase-kt` modules for auth, PostgREST, storage, and functions, in `com.contractproof.data` inside `composeApp` `commonMain`.

## PostgreSQL

- **Purpose:** the relational store for organizations, contracts, jobs, evidence, disputes, and reports.
- **Why:** the product is multi-tenant and relational. Row-level security is the tenant boundary.
- **Priority:** P0. Server only.
- **Where:** Supabase Postgres. The app never opens a database connection. Access is PostgREST plus RLS. Migrations live under `supabase/` when that phase starts.

## RevenueCat

- **Purpose:** subscriptions, restore, and entitlements.
- **Why:** the hackathon requires RevenueCat for the purchase. The app does not run its own payment processor. This APK is not on the Play Store, so purchases use the RevenueCat Test Store.
- **Priority:** P0. Not added yet.
- **Where:** the Android Purchases SDK lives in `androidMain`. Feature code calls a domain entitlement port in `commonMain`. The UI does not read a hard-coded plan name.

Entitlements the port exposes:

- Free: 1 location. Contract extraction and dispute PDFs stay locked.
- Pro: $49/month.
- Business: $99/month.

A demo flag may hide the paywall for a recording. The judge build keeps the real paywall, including restore.

## AI integration

- **Purpose:** turn contract text into schema-valid requirement JSON, and later classify exceptions and summarize disputes.
- **Why:** contract language is unstructured. The model must not become contractual truth, and its keys must not ship in the APK.
- **Priority:** P0. Server only. No model SDK in the app.
- **Where:** one Supabase Edge Function. It extracts PDF text, OCRs pages with no text layer, calls Gemini, and calls Groq only when Gemini fails or the free-tier quota is exhausted. The JSON is stored as an extraction until an owner approves it.

## PDF generation

- **Purpose:** build the service evidence report.
- **Why:** the report must be reproducible from database records. Generating it on the server keeps the APK smaller and uses the same records the owner sees.
- **Priority:** P0. Server only. No PDF library in the app.
- **Where:** the report Edge Function uses `pdf-lib` and writes the file to the private reports bucket.

## Camera

- **Purpose:** capture evidence photos for a service requirement.
- **Why:** evidence is taken at the job site. CameraX is the Android camera API. Domain code never calls it.
- **Priority:** P0. Android only for this MVP.
- **Where:** CameraX in `androidMain`, behind a shared capture interface in `commonMain`. A later iOS actual uses AVFoundation.

## Location

- **Purpose:** optional supporting evidence at check-in or photo capture.
- **Why:** location is not the product. A single platform fix is enough. `LocationManager` works on a sideloaded APK without Play Services. There is no continuous tracking.
- **Priority:** P1.
- **Where:** `androidMain`, behind a shared location interface. Domain code receives a coordinate or an absence. It does not call the platform API.

## Analytics

- **Purpose:** product events such as contract upload, evidence added, dispute opened, and paywall viewed.
- **Why:** PostHog is the selected product analytics tool. It is not required to demonstrate the golden path.
- **Priority:** P2. No SDK in this step.
- **Where:** a small event interface in `com.contractproof.core`. The PostHog implementation is added only when a later prompt asks for it. Events must not include contract text or evidence images.

## Crash reporting

- **Purpose:** crashes and failures in sync, upload, AI, and report generation.
- **Why:** Sentry is the selected crash reporter. The golden path can ship without it.
- **Priority:** P2. No SDK in this step.
- **Where:** a hook beside the analytics interface in `core`. Secrets and contract contents stay out of reports.

## Notifications

- **Purpose:** later alerts for assigned work, missing evidence, and new disputes.
- **Why:** Firebase Cloud Messaging is the Android push channel. The demo does not depend on push, and this project has no Firebase app yet.
- **Priority:** P2. No Firebase project in this step.
- **Where:** a future `androidMain` implementation. iOS would use APNs later. Domain code reacts to a notification payload through a shared interface, not through Firebase types.

## Testing

- **Purpose:** lock evidence coverage, service completion, exception acceptance, dispute reconstruction, and entitlement checks.
- **Why:** those rules must fail in tests before they fail in a demo. They do not need a device, SQLDelight, or Supabase.
- **Priority:** P0 for domain tests. P1 for Compose UI tests.
- **Where:** `kotlin.test` in `composeApp` `commonTest`, with in-memory fakes of the repository interfaces. Compose UI tests are added with the screen that needs them. XCTest waits until the iOS target exists.

## Not introduced by this document

Navigation, Koin, SQLDelight, `supabase-kt`, the RevenueCat Android SDK, CameraX, PostHog, Sentry, and Firebase are not added to the Gradle project by this document. Server libraries (`pdf-lib`, the Gemini client, and the Groq client) are not added either. The next feature prompt adds only the dependency it needs.
