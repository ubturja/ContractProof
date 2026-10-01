# extract-contract

Owner-only Edge Function that reads a contract version PDF, extracts text, calls Gemini (Groq fallback), validates **ContractExtractionV1**, and stores the result on `contract_versions.extraction` with `status = extracted`. It does **not** insert `contract_requirements` or approve the contract.

## Secrets

Set in the Supabase project (or `supabase secrets set`):

| Name | Required |
| --- | --- |
| `SUPABASE_URL` | yes (auto in Edge) |
| `SUPABASE_ANON_KEY` | yes (auto in Edge) |
| `SUPABASE_SERVICE_ROLE_KEY` | yes (auto in Edge) |
| `GEMINI_API_KEY` | yes for primary LLM |
| `GROQ_API_KEY` | optional fallback |
| `GEMINI_MODEL` | optional, default `gemini-2.0-flash` |
| `GROQ_MODEL` | optional, default `llama-3.3-70b-versatile` |

## Request

```http
POST /functions/v1/extract-contract
Authorization: Bearer <user access token>
Content-Type: application/json

{ "contract_version_id": "<uuid>", "force": false }
```

## Success

```json
{
  "ok": true,
  "contract_version_id": "...",
  "status": "extracted",
  "requirement_count": 3,
  "visit_count": 1,
  "warnings": []
}
```

## Error codes

`UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `VERSION_NOT_IN_REVIEW`, `NO_PDF`, `STORAGE_DOWNLOAD_FAILED`, `TEXT_EXTRACTION_FAILED`, `LLM_UNAVAILABLE`, `LLM_OUTPUT_INVALID`, `VALIDATION_FAILED`, `DATABASE_UPDATE_FAILED`

Each error includes `retryable: boolean`.

## Local

```bash
cd supabase/functions/extract-contract
deno task test
supabase functions serve extract-contract --env-file ../../.env.local
```

Schema: [`../_shared/extraction-v1.schema.json`](../_shared/extraction-v1.schema.json)
