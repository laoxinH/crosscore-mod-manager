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

-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.conscrypt.**
-dontwarn androidx.window.**
-dontwarn com.squareup.okhttp.**

-keep class net.sf.sevenzipjbinding.** { *; }
-keep class top.laoxin.** { *; }
-keep class top.lings.** { *; }
-keep class okhttp3.** { *; }

-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.* <fields>;
}
