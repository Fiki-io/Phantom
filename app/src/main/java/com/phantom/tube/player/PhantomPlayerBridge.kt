package com.phantom.tube.player

import android.webkit.JavascriptInterface

class PhantomPlayerBridge(
    var onReadyCallback: () -> Unit = {},
    var onStateChangeCallback: (Int) -> Unit = {},
    var onTimeUpdateCallback: (Float, Float, Float) -> Unit = { _, _, _ -> },
    var onErrorCallback: (Int) -> Unit = {}
) {
    @JavascriptInterface
    fun onReady() {
        onReadyCallback()
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        onStateChangeCallback(state)
    }

    @JavascriptInterface
    fun onTimeUpdate(currentTime: Float, duration: Float, bufferedFraction: Float) {
        onTimeUpdateCallback(currentTime, duration, bufferedFraction)
    }

    @JavascriptInterface
    fun onError(errorCode: Int) {
        onErrorCallback(errorCode)
    }
}
