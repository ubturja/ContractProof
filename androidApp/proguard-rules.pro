# ContractProof release — conservative keeps; tighten after smoke tests.

-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions

# Kotlin / coroutines
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.contractproof.**$$serializer { *; }
-keepclassmembers class com.contractproof.** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Ktor / networking
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Supabase Kotlin client
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Sentry / PostHog (optional SDKs)
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**

# SQLDelight
-keep class com.contractproof.data.local.** { *; }

# Compose (default rules from dependencies usually suffice)
-dontwarn androidx.compose.**
