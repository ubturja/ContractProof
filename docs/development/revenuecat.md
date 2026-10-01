# RevenueCat (Android)

Subscription billing is optional at build time. Without keys the app runs in **free-only** mode and does not initialize the RevenueCat SDK.

## Local configuration

Add to `local.properties` (not committed):

```properties
REVENUECAT_API_KEY=
REVENUECAT_ENTITLEMENT_PRO=pro
REVENUECAT_ENTITLEMENT_BUSINESS=business
```

You may also set `REVENUECAT_API_KEY` via environment variable or Gradle property.

`DEMO_BYPASS_SUBSCRIPTION=true` treats the signed-in org as **Pro** for recordings only. Default is off for judge builds.

## Test Store

Use a RevenueCat Test Store offering with packages identifiable as `pro` and `business` (package identifier or product id). Purchases require a configured API key and an Android build with Google Play / Test Store setup per RevenueCat docs.
