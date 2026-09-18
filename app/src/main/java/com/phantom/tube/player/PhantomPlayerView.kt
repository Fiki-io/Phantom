package com.phantom.tube.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class PhantomPlayerController(context: Context) {
    private var webView: WebView? = null
    private var isBridgeReady = false
    private var pendingVideoId: String? = null
    private var pendingStartSeconds: Float = 0f

    @SuppressLint("SetJavaScriptEnabled")
    fun attachWebView(
        view: WebView,
        bridge: PhantomPlayerBridge,
        videoId: String = "",
        startSeconds: Float = 0f
    ) {
        this.webView = view
        view.setBackgroundColor(Color.BLACK)

        // Enable cookies and third-party cookies for seamless YouTube embed session
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(view, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            allowContentAccess = true

            // Use clean Chrome mobile User-Agent (strips WebView indicators to prevent Google embed blocks)
            val defaultUa = userAgentString
            if (defaultUa != null) {
                userAgentString = defaultUa
                    .replace("; wv", "")
                    .replace("Version/4.0 ", "")
            }
        }

        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                android.util.Log.d("PhantomJS", "${consoleMessage?.message()} (${consoleMessage?.lineNumber()})")
                return true
            }
        }

        view.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                android.util.Log.e("PhantomWeb", "Resource error: ${error?.description}")
            }
        }

        view.addJavascriptInterface(bridge, "PhantomBridge")

        // Load HTML with base URL 'https://www.youtube.com' to provide valid Origin & Referer headers
        val rawHtml = try {
            view.context.assets.open("player.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        val htmlContent = rawHtml
            .replace("__VIDEO_ID__", videoId)
            .replace("__START_SECONDS__", startSeconds.toInt().toString())

        view.loadDataWithBaseURL(
            "https://www.youtube.com",
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    fun markBridgeReady() {
        isBridgeReady = true
        pendingVideoId?.let { id ->
            loadVideo(id, pendingStartSeconds)
            pendingVideoId = null
            pendingStartSeconds = 0f
        }
    }

    fun loadVideo(videoId: String, startSeconds: Float = 0f) {
        if (!isBridgeReady) {
            pendingVideoId = videoId
            pendingStartSeconds = startSeconds
            return
        }
        evaluateJs("window.loadVideo('$videoId', $startSeconds);")
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

    fun setPlaybackQuality(quality: String) {
        evaluateJs("window.setPlaybackQuality('$quality');")
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
        isBridgeReady = false
    }
}

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
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                controller.attachWebView(this, bridge, videoId, startSeconds)
            }
        },
        update = { /* controller maintains internal state */ }
    )
}
