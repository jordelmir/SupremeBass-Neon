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
