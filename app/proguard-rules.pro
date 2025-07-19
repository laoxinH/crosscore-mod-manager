-allowaccessmodification
-repackageclasses ''

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# kotlinx.serialization rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class top.laoxin.modmanager.**$$serializer { *; }
-keepclassmembers class top.laoxin.modmanager.** {
    *** Companion;
}
-keepclasseswithmembers class top.laoxin.modmanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.conscrypt.**
-dontwarn androidx.window.**
-dontwarn com.squareup.okhttp.**

-keep class net.sf.sevenzipjbinding.** { *; }
-keep class top.laoxin.** { *; }
-keep class okhttp3.** { *; }

-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.* <fields>;
}

-keep class dagger.** { *; }
-keep interface dagger.** { *; }
-keep class androidx.lifecycle.** { *; }

-keep class dagger.hilt.** { *; }

-keep class * extends androidx.lifecycle.ViewModel { *; }

-keep class **.Hilt_* { *; }
-keep class **.HiltInjector { *; }

-keep class javax.inject.** { *; }
-keep class com.google.inject.** { *; }

-keep @dagger.hilt.android.HiltAndroidApp class *