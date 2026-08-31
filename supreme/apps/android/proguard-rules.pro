# Supreme ProGuard Rules

# ==================
# Room
# ==================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room DAOs
-keep class com.supreme.android.data.** { *; }

# ==================
# Compose
# ==================
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ==================
# ML Kit
# ==================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ==================
# Coroutines
# ==================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ==================
# Kotlin Serialization
# ==================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ==================
# Supreme Core Models
# ==================
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

# ==================
# Keep data class members
# ==================
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================
# Android Lifecycle
# ==================
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ==================
# Navigation
# ==================
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable
