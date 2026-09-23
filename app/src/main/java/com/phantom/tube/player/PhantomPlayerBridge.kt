package com.phantom.tube.player

import android.webkit.JavascriptInterface

class PhantomPlayerBridge(
    var onReadyCallback: () -> Unit = {},
    var onStateChangeCallback: (Int) -> Unit = {},
    var onTimeUpdateCallback: (String, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    var onErrorCallback: (Int) -> Unit = {},
    var onQualityChangeCallback: (String, List<String>) -> Unit = { _, _ -> }
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
    fun onTimeUpdate(videoId: String, currentTime: Float, duration: Float, bufferedFraction: Float) {
        onTimeUpdateCallback(videoId, currentTime, duration, bufferedFraction)
    }

    @JavascriptInterface
    fun onQualityChange(currentQuality: String, availableQualitiesJson: String) {
        val list = try {
            val jsonArray = org.json.JSONArray(availableQualitiesJson)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            availableQualitiesJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        onQualityChangeCallback(currentQuality, list)
    }

    @JavascriptInterface
    fun onError(errorCode: Int) {
        onErrorCallback(errorCode)
    }
}
