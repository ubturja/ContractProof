# Contract extraction test fixtures

Deterministic `ContractExtractionV1` payloads for unit tests (Deno `validateExtraction` and Kotlin `ExtractionRules`). No live LLM.

| File | Scenario | `validateExtraction` |
| --- | --- | --- |
| `normal_contract.json` | Happy path: visit + 2 requirements | Accept |
| `missing_evidence_requirement.json` | Photo-critical task with `requires_photo: false` + warning | Accept (owner must correct in review) |
| `ambiguous_frequency.json` | Empty visits + ambiguous cadence warning | Accept (owner must set schedule) |
| `unsupported_wording.json` | Vague task + normalization warning | Accept (owner should refine task text) |
| `missing_fields.json` | Omits `requirements` array | Reject (schema) |
| `duplicate_requirements.json` | Duplicate requirement `key` | Reject (business rules) |
| `invalid_requirement_types.json` | `requires_photo` not boolean | Reject (schema) |
| `human_corrections.json` | Valid baseline for edit-before-approve tests | Accept |

Malformed model output (truncated JSON, broken markdown fences) is covered inline in `extract-contract/validate_test.ts` via `parseModelJson`, not as JSON files in this folder.

Semantic warnings in `document.warnings` do **not** fail validation; they are surfaced in mobile review until the owner approves edited requirements.
