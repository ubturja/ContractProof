# Contributing

Thank you for your interest in ContractProof.

## How to contribute

1. Open an issue to discuss larger changes.
2. Fork the repository and create a focused branch.
3. Keep pull requests small and testable.
4. Run `./gradlew :composeApp:testAndroidHostTest` and relevant Android tests when you touch shared logic.

## Security and secrets

- Do **not** commit `local.properties`, `keystore.properties`, `.env`, keystores (`.jks`), or `google-services.json`.
- Do **not** commit Supabase service role keys, AI provider keys, or RevenueCat secret keys.
- The mobile app only embeds the Supabase **anon** key and RevenueCat **public** SDK key at build time.

See [SECURITY.md](SECURITY.md) for reporting vulnerabilities.
