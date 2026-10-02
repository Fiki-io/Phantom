# Phantom Proguard & Obfuscation Rules

# 1. Native JNI & JSInterface Protection
-keepclassmembers class * {
    native <methods>;
}
-keep class com.phantom.tube.core.security.PhantomNative { *; }

-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.phantom.tube.player.PhantomPlayerBridge { *; }

# 2. Data Models & Database
-keep class com.phantom.tube.data.model.** { *; }
-keep class com.phantom.tube.core.database.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# 3. Obfuscation & Deep Bytecode Optimization
-repackageclasses ''
-allowaccessmodification
-renamesourcefileattribute ''
-optimizationpasses 5
-overloadaggressively
# Eliminate Kotlin null check assertions in release bytecode
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void throwNpe(...);
    public static void throwJavaNpe(...);
    public static void throwUninitializedPropertyAccessException(...);
    public static void throwAssert(...);
    public static void throwIllegalArgument(...);
    public static void throwIllegalState(...);
}
