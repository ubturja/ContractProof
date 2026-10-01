# Firebase Cloud Messaging (Android)

Push is optional. Without `androidApp/google-services.json`, Firebase is not initialized and the app runs normally.

## Setup

1. Create a Firebase project and add an Android app with package `com.contractproof.app`.
2. Download `google-services.json` into `androidApp/google-services.json` (not committed).
3. Rebuild the Android app.

## Payload contract

Data-only FCM messages should include:

| `type` | Required fields |
| --- | --- |
| `assigned_service` | `job_id` |
| `missing_evidence` | `job_id` |
| `dispute_received` | `dispute_id` |
| `report_generated` | `dispute_id` |

Do not include contract text, evidence, or personal data in payloads.

## Owner alerts on client disputes

When a client files a dispute, the app calls the `notify-dispute-received` edge function (best-effort). It sends data-only `dispute_received` messages to FCM tokens stored in `push_device_tokens` for organization owners. Set `FCM_SERVER_KEY` in the function environment; without it the function no-ops and the dispute still saves.

Android registers tokens via `PushTokenBridge` after sign-in when FCM delivers a token.

## iOS

APNs will use the same `type` and id fields through a future `iosMain` implementation. No APNs certificates are required for this MVP step.
