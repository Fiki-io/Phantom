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

# Gson Serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# 3. Obfuscation Hardening against AI & Decompilation
# Flattens all internal classes into root package so architecture is hidden
-repackageclasses ''
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
