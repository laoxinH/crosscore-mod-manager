-allowaccessmodification
-repackageclasses

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

-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.conscrypt.Conscrypt
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.Sidecar*

-keep class net.sf.sevenzipjbinding.** { *; }
-keep class top.laoxin.modmanager.** { *; }
-keep class okhttp3.** { *; }
-keep class com.squareup.okhttp.** { *; }

-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.Expose *;
}
