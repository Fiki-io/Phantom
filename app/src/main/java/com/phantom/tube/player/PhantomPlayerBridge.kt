package com.phantom.tube.player

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class PhantomPlayerBridge(
    private val onReadyCallback: () -> Unit,
    private val onStateChangeCallback: (Int) -> Unit,
    private val onTimeUpdateCallback: (Float, Float, Float) -> Unit,
    private val onErrorCallback: (Int) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onReady() {
        mainHandler.post {
            onReadyCallback()
        }
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        mainHandler.post {
            onStateChangeCallback(state)
        }
    }

    @JavascriptInterface
    fun onTimeUpdate(currentTime: Double, duration: Double, bufferedFraction: Double) {
        mainHandler.post {
            onTimeUpdateCallback(
                currentTime.toFloat(),
                duration.toFloat(),
                bufferedFraction.toFloat()
            )
        }
    }

    @JavascriptInterface
    fun onError(errorCode: Int) {
        mainHandler.post {
            onErrorCallback(errorCode)
        }
    }
}
