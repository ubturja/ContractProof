# Observability (PostHog and Sentry)

Keys are optional. Empty configuration disables analytics and error reporting without affecting app startup.

## local.properties

```properties
POSTHOG_API_KEY=
POSTHOG_HOST=https://us.i.posthog.com
SENTRY_DSN=
SENTRY_ENVIRONMENT=development
```

Environment defaults to `development` when `SENTRY_DEBUG_BUILD` is true (Gradle property, default true) and `production` otherwise, unless `SENTRY_ENVIRONMENT` is set.

Events intentionally exclude contract text, evidence payloads, and personal identifiers beyond organization and entity UUIDs.
