# Security

## Reporting a vulnerability

If you believe you have found a security issue, please open a private report through GitHub Security Advisories for this repository, or contact the maintainers directly. Do not file public issues for exploitable vulnerabilities.

## What must never be committed

- Supabase **service role** key or database passwords
- Gemini, Groq, or other **AI API keys** (Edge Functions only)
- RevenueCat **secret** / webhook keys (server only)
- **Signing** keystores (`*.jks`, `*.keystore`) and `keystore.properties`
- Real customer contracts, PII, or production evidence files
- `local.properties`, `.env`, and `androidApp/google-services.json`

The Android APK intentionally contains the Supabase **anon** key and RevenueCat **public** SDK key. Tenant isolation relies on **Row Level Security** and the user’s session JWT, not on hiding the anon key.

## Mobile release builds

- Demo subscription bypass and login credential hints are disabled in non-debuggable release builds.
- Use an upload keystore via `keystore.properties` for judge APKs; do not distribute debug-signed release builds.

More detail: [docs/audits/security-mvp.md](docs/audits/security-mvp.md).
