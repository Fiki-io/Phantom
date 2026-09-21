# Phantom Proguard Rules
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.phantom.tube.player.PhantomPlayerBridge { *; }
-keep class com.phantom.tube.data.model.** { *; }
-keep class com.phantom.tube.core.database.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Gson
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
