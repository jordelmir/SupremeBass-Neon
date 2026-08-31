# Supreme ProGuard Rules

# Keep data classes
-keep class com.supreme.core.** { *; }
-keep class com.supreme.fix.** { *; }
-keep class com.supreme.maintenance.** { *; }
-keep class com.supreme.warranty.** { *; }
-keep class com.supreme.network.** { *; }
-keep class com.supreme.noise.** { *; }
-keep class com.supreme.vibration.** { *; }
-keep class com.supreme.camera.** { *; }
-keep class com.supreme.find.** { *; }
-keep class com.supreme.home.** { *; }
-keep class com.supreme.utilities.** { *; }
-keep class com.supreme.inventory.** { *; }
-keep class com.supreme.vehicle.** { *; }
-keep class com.supreme.leak.** { *; }
-keep class com.supreme.emergency.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
