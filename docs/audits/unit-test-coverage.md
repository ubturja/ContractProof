# Unit test coverage matrix (Prompt 63)

Maps required business areas to primary code and `commonTest` suites. Severity reflects gaps **before** P63 additions.

| Area | Primary code | Test suite(s) | Notes |
|------|----------------|---------------|--------|
| Authentication state | `AuthController`, `SessionController` | `AuthControllerTest`, `SessionControllerTest` | Session restore, sign-up, company setup, sign-out |
| Authorization | `Access`, `NotificationRoutingRules` | `AccessTest`, `NotificationRoutingRulesTest` | Role capabilities + FCM routing |
| Contract requirements | `RequirementRules`, `RequirementsController` | `RequirementRulesTest`, `RequirementsControllerTest` | |
| Requirement normalization | `ExtractionRules` | `ExtractionRulesTest`, fixtures | |
| Evidence completeness | `EvidenceCompletenessEngine` | `EvidenceCompletenessEngineTest` | |
| Service completion | `ServiceCompletionRules`, `ServiceExecutionRules` | `ServiceCompletionRulesTest`, `ServiceExecutionRulesTest` | Pending upload vs missing |
| Exceptions | `ExceptionRules`, `OfflineFirstExceptionGateway` | `ExceptionRulesTest`, `OfflineFirstExceptionGatewayTest` | |
| Dispute reconstruction | `DisputeReconstructionRules` | `DisputeReconstructionRulesTest` | |
| Subscription entitlements | `SubscriptionEntitlementRules`, `Entitlements` | `SubscriptionEntitlementRulesTest`, `EntitlementsTest`, `PaywallControllerTest` | |
| Offline queue | `SyncQueueRules`, `SqlDelightSyncQueueStore` | `SyncQueueRulesTest`, `SqlDelightSyncQueueStoreTest` | |
| Synchronization | `SyncCoordinator`, offline repos | `SyncCoordinatorTest`, `OfflineFirstServiceJobRepositoryTest` | Retry on upload failure |
| Report generation | `ReportRules`, `EvidenceReportAssemblyRules` | `ReportRulesTest`, `EvidenceReportAssemblyRulesTest`, `ReportPreviewControllerTest` | |

Run: `./gradlew :composeApp:testAndroidHostTest`
