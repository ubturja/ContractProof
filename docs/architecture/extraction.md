# Contract extraction (AI pipeline)

Model output is **not** contractual truth. It is stored on `contract_versions.extraction` until an owner reviews and approves it. Approval copies edited rows into `contract_requirements` and sets the version `approved`. The extraction Edge Function never inserts into `contract_requirements` and never sets `approved`.

## Flow

1. Mobile (later) calls the `extract-contract` Edge Function with a JWT and `contract_version_id`.
2. The function downloads the version PDF from the private `contracts` bucket.
3. Text is extracted per page into Markdown. Pages with no text layer are listed in `document.ocr_page_indexes` (OCR is not run in MVP; those pages produce warnings).
4. Gemini produces JSON matching **ContractExtractionV1**. Groq is used only when Gemini fails or quota is exhausted.
5. JSON Schema and business rules validate the payload.
6. On success: `contract_versions.extraction` is set and `status` becomes `extracted`. On failure: `status` stays `uploaded` and `extraction` stays null.

## ContractExtractionV1

Canonical JSON Schema: [`supabase/functions/_shared/extraction-v1.schema.json`](../../supabase/functions/_shared/extraction-v1.schema.json).

| Field | Meaning |
| --- | --- |
| `schema_version` | Always `1` for this shape. |
| `status` | Always `completed` when persisted. Failed runs are not stored. |
| `extracted_at` | ISO-8601 timestamp when validation passed. |
| `pipeline` | Which text engine and LLM produced the result. |
| `document` | Page count, text size, pages needing OCR, non-fatal warnings. |
| `visits` | Weekly schedule **candidates** (same fields as `service_schedules`, plus optional `confidence`). |
| `requirements` | Task **candidates** with stable `key` (future `extraction_key`), photo/mandatory flags, optional zone and quote. |

## API

**Request:** `{ "contract_version_id": "<uuid>", "force": false }`

**Success:** `{ "ok": true, "contract_version_id", "status": "extracted", "requirement_count", "visit_count", "warnings" }`

**Error:** `{ "ok": false, "error": { "code", "message", "retryable", "details?" } }`

Stable error codes are listed in [`supabase/functions/extract-contract/README.md`](../../supabase/functions/extract-contract/README.md).

## Kotlin

Domain DTOs and `ExtractionRules` in `composeApp` mirror this schema for unit tests and a future mobile gateway. No review UI in the extraction prompt.
