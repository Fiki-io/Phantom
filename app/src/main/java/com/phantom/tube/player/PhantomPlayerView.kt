package com.phantom.tube.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

class PhantomPlayerController(context: Context) {
    private var webView: WebView? = null
    private var isReady = false
    private var pendingVideoId: String? = null
    private var pendingStartSeconds: Float = 0f

    fun attachWebView(view: WebView) {
        this.webView = view
    }

    fun onReady() {
        isReady = true
        val targetId = pendingVideoId
        if (targetId != null) {
            loadVideo(targetId, pendingStartSeconds)
            pendingVideoId = null
            pendingStartSeconds = 0f
        }
    }

    fun loadVideo(videoId: String, startSeconds: Float = 0f) {
        if (isReady && webView != null) {
            evaluateJs("window.loadVideo('$videoId', $startSeconds);")
        } else {
            pendingVideoId = videoId
            pendingStartSeconds = startSeconds
        }
    }

    fun play() {
        evaluateJs("window.playVideo();")
    }

    fun pause() {
        evaluateJs("window.pauseVideo();")
    }

    fun seekTo(seconds: Float) {
        evaluateJs("window.seekTo($seconds);")
    }

    fun setPlaybackRate(rate: Float) {
        evaluateJs("window.setPlaybackRate($rate);")
    }

    private fun evaluateJs(script: String) {
        webView?.post {
            webView?.evaluateJavascript(script, null)
        }
    }

    fun release() {
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        webView = null
        isReady = false
        pendingVideoId = null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PhantomGhostSurface(
    videoId: String,
    startSeconds: Float = 0f,
    modifier: Modifier = Modifier,
    controller: PhantomPlayerController,
    bridge: PhantomPlayerBridge
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                setBackgroundColor(Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = false
                    allowContentAccess = false

                    // Clean Chrome User-Agent
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }

                webChromeClient = WebChromeClient()

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }
                }

                addJavascriptInterface(bridge, "PhantomBridge")
                controller.attachWebView(this)

                // Load with official HTTPS domain via WebViewAssetLoader
                val startInt = startSeconds.toInt()
                loadUrl("https://appassets.androidplatform.net/assets/player.html?v=$videoId&start=$startInt")
            }
        },
        update = { /* controller maintains internal state */ }
    )
}
