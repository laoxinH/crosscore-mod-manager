# ===============================
# Basic ProGuard Optimization Rules
# ===============================
-allowaccessmodification
-repackageclasses ''

# ===============================
# Android System Components
# ===============================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===============================
# Native Method Protection
# ===============================
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ===============================
# Annotation and Metadata Preservation
# ===============================
-keepattributes *Annotation*, InnerClasses
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes AnnotationDefault

# ===============================
# Kotlinx Serialization Rules
# ===============================
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class top.laoxin.modmanager.**$$serializer { *; }
-keepclassmembers class top.laoxin.modmanager.** {
    *** Companion;
}
-keepclasseswithmembers class top.laoxin.modmanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepnames class <1>$$serializer {
    static <1>$$serializer INSTANCE;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$Companion *;
}
-keepnames class <2>$Companion

-keep @kotlinx.serialization.Serializable class * {
    *** serializer();
}

# ===============================
# Third-Party Library Protection
# ===============================
-keep class net.sf.sevenzipjbinding.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }

# ===============================
# Project-Specific Classes
# ===============================
-keep class top.laoxin.** { *; }

# ===============================
# JSON Serialization (Gson)
# ===============================
-keepclassmembers class * {
    @com.google.gson.annotations.* <fields>;
}

# ===============================
# Dependency Injection (Dagger/Hilt)
# ===============================
-keep class dagger.** { *; }
-keep interface dagger.** { *; }
-keep class dagger.hilt.** { *; }
-keep class **.Hilt_* { *; }
-keep class **.HiltInjector { *; }
-keep class javax.inject.** { *; }
-keep class com.google.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *

# ===============================
# Android Architecture Components
# ===============================
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ===============================
# Warning Suppressions
# ===============================
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.conscrypt.**
-dontwarn androidx.window.**
-dontwarn com.squareup.okhttp.**