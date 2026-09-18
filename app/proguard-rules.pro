# Phantom Proguard Rules
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.phantom.tube.player.PhantomPlayerBridge { *; }
-keep class com.phantom.tube.data.model.** { *; }
-keep class com.phantom.tube.core.database.** { *; }
