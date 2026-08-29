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

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.supremecorp.bass.data.device.** { *; }
-keep class com.supremecorp.bass.data.experiment.** { *; }

# ─── Oboe / JNI ───
-keep class com.supremecorp.bass.audio.backend.NativeDsp { *; }
-keep class com.supremecorp.bass.audio.backend.NativeDsp$Companion { *; }
-dontwarn com.google.oboe.**

# ─── Signal Engine Domain Models ───
-keep class com.supremecorp.bass.domain.model.** { *; }

# ─── DSP Components (keep for reflection/runtime) ───
-keep class com.supremecorp.bass.dsp.** { *; }
-keep class com.supremecorp.bass.audio.input.** { *; }

# ─── Enums ───
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Parcelable ───
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ─── Serializable ───
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ─── CameraX ───
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ─── Lifecycle ───
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ─── Coroutines ───
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ─── App classes ───
-keep class com.supremecorp.bass.MainActivity { *; }
-keep class com.supremecorp.bass.AudioService { *; }

# ─── Remove logging in release ───
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# ─── Optimization flags ───
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
