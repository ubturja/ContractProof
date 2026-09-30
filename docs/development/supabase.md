# Supabase configuration

The Android app creates one Supabase client at startup. Sign-in is email and password through the Auth plugin. The plugin stores the session on the device and restores it the next time the app starts. There is no social login. Set two values on your machine, then rebuild.

## Required values

- `SUPABASE_URL`: the project URL. It must start with `https://`. Copy it from the Supabase project settings.
- `SUPABASE_ANON_KEY`: the project’s anon public key. The app is allowed to hold this key. It is compiled into the debug APK.

The app reads them in this order:

1. Environment variables of the same names.
2. The same names in `local.properties` at the repository root.

`local.properties` is gitignored. Do not commit either value, and do not put them in `gradle.properties`.

Example `local.properties` entries, with placeholders only:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

A Gradle `-P` property of the same name is used only when both of the places above are unset. Use that for a one-off build. Do not save a real key in a committed script.

After changing either value, run `./gradlew :androidApp:assembleDebug` again. The generated file lives under `composeApp/build/`, which is not committed. If a value is missing, or the URL is not `https`, that build stops and points here.

## Values that stay off the device

Do not put these in the app, in `local.properties`, or in git:

- the Supabase service-role key
- the RevenueCat webhook secret
- Gemini or Groq keys

Those belong in Edge Function secrets. The client logs the URL host only. It does not log the anon key.
