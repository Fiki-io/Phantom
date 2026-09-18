package com.phantom.tube.player

import android.webkit.JavascriptInterface

class PhantomPlayerBridge(
    private val onReadyCallback: () -> Unit,
    private val onStateChangeCallback: (Int) -> Unit,
    private val onTimeUpdateCallback: (Float, Float, Float) -> Unit,
    private val onErrorCallback: (Int) -> Unit
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
