# 项目特定的 ProGuard 规则

# 保留枚举类的值和 valueOf 方法
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable 的 CREATOR 字段
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# 忽略特定类的警告
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.conscrypt.Conscrypt
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.Sidecar*

# 保留行号信息以便调试
-keepattributes LineNumberTable

# 允许访问修改和重新打包类
-allowaccessmodification
-repackageclasses

# 保留特定包下的所有类和成员
-keep class net.sf.sevenzipjbinding.** { *; }
-keep class top.laoxin.modmanager.** { *; }
-keep class com.example.model.** { *; }
-keep class okhttp3.** { *; }
-keep class com.squareup.okhttp.** { *; }
-keep class okhttp3.Call { *; }
-keep class okhttp3.Interceptor { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Response { *; }
-keep class okhttp3.internal.** { *; }

# 保留所有类的名称和成员
-keepnames class * { *; }
-keepclassmembers class * { *; }

# 保证 Gson 不被混淆
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.Expose *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.Since *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.Until *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.JsonAdapter *;
}

