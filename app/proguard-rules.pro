# Supreme Bass - ProGuard Rules

# ─── Google AdMob / Play Services ───
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.ads.**

# ─── Compose ───
-dontwarn androidx.compose.**

# ─── Kotlin ───
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
