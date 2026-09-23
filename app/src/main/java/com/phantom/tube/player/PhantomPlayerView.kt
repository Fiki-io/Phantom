package com.phantom.tube.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.os.Looper
import android.view.View
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

class PhantomBackgroundWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        // Enforce GPU hardware acceleration layer for smooth 60/120fps video rendering
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        // Intercept GONE/INVISIBLE to keep Chromium audio and timers running in background / screen off
        super.onWindowVisibilityChanged(View.VISIBLE)
    }

    override fun onPause() {
        // Do NOT pause Chromium media or timers when activity is in background
        resumeTimers()
    }
}

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

    fun setLoop(loop: Boolean) {
        evaluateJs("window.setLoop($loop);")
    }

    private fun evaluateJs(script: String) {
        val target = webView ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            target.evaluateJavascript(script, null)
        } else {
            target.post { target.evaluateJavascript(script, null) }
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

            PhantomBackgroundWebView(ctx).apply {
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
                    offscreenPreRaster = true

                    // Clean Chrome User-Agent
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }

                webChromeClient = WebChromeClient()

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val assetResp = assetLoader.shouldInterceptRequest(request.url)
                        if (assetResp != null) return assetResp

                        val cleanResp = PhantomIFrameCleanEngine.intercept(request)
                        if (cleanResp != null) return cleanResp

                        return super.shouldInterceptRequest(view, request)
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

object PhantomIFrameCleanEngine {
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val cssCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()

    val CSS_RULES = """
        .ytp-chrome-top,
        .ytp-title,
        .ytp-title-text,
        .ytp-title-channel,
        .ytp-show-cards-title,
        .ytp-share-button,
        .ytp-share-panel,
        .ytp-overflow-button,
        a.ytp-title-link,
        .ytp-chrome-bottom,
        .ytp-watermark,
        .ytp-youtube-button,
        a.ytp-youtube-button,
        .ytp-gradient-top,
        .ytp-gradient-bottom,
        .ytp-large-play-button,
        .ytp-large-play-button-bg,
        .ytp-play-button,
        .ytp-pause-overlay,
        .ytp-pause-overlay-container,
        .ytp-endscreen-content,
        .ytp-ce-element,
        .ytp-bezel,
        .ytp-bezel-text,
        .ytp-bezel-icon,
        .ytp-cued-thumbnail-overlay,
        .ytp-cued-thumbnail-overlay-image,
        .ytp-spinner,
        .ytp-contextmenu,
        .ytp-paid-content-overlay,
        .ytp-offline-slate,
        .ytp-suggested-action-badge {
            display: none !important;
            opacity: 0 !important;
            visibility: hidden !important;
            pointer-events: none !important;
            width: 0 !important;
            height: 0 !important;
        }
    """.trimIndent()

    val STYLE_TAG = "<style id=\"phantom-clean-engine\">$CSS_RULES</style>"

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        if (request.method != "GET") return null

        // 1. Intercept CSS files (e.g. www-player.css)
        if (url.contains("/www-player.css") || (url.contains("youtube.com/s/player/") && url.contains(".css"))) {
            try {
                val cached = cssCache[url]
                if (cached != null) {
                    val headers = mapOf(
                        "Content-Type" to "text/css; charset=utf-8",
                        "Access-Control-Allow-Origin" to "*"
                    )
                    return WebResourceResponse("text/css", "utf-8", 200, "OK", headers, java.io.ByteArrayInputStream(cached))
                }

                val reqBuilder = okhttp3.Request.Builder().url(url)
                for ((k, v) in request.requestHeaders) {
                    reqBuilder.header(k, v)
                }
                val resp = httpClient.newCall(reqBuilder.build()).execute()
                if (resp.isSuccessful) {
                    val rawCss = resp.body?.string() ?: ""
                    val modifiedCss = rawCss + "\n\n/* Phantom Clean Engine */\n" + CSS_RULES
                    val bytes = modifiedCss.toByteArray(Charsets.UTF_8)
                    cssCache[url] = bytes
                    val headers = mapOf(
                        "Content-Type" to "text/css; charset=utf-8",
                        "Access-Control-Allow-Origin" to "*"
                    )
                    return WebResourceResponse("text/css", "utf-8", 200, "OK", headers, java.io.ByteArrayInputStream(bytes))
                }
            } catch (e: Exception) {
                // Fallback to WebView default network
            }
        }

        // 2. Intercept YouTube embed HTML
        if (url.contains("youtube.com/embed/")) {
            try {
                val reqBuilder = okhttp3.Request.Builder().url(url)
                for ((k, v) in request.requestHeaders) {
                    reqBuilder.header(k, v)
                }
                val resp = httpClient.newCall(reqBuilder.build()).execute()
                if (resp.isSuccessful) {
                    val rawHtml = resp.body?.string() ?: ""
                    val modifiedHtml = if (rawHtml.contains("</head>")) {
                        rawHtml.replace("</head>", "$STYLE_TAG</head>")
                    } else if (rawHtml.contains("<body")) {
                        rawHtml.replace("<body", "$STYLE_TAG<body")
                    } else {
                        STYLE_TAG + rawHtml
                    }
                    val bytes = modifiedHtml.toByteArray(Charsets.UTF_8)
                    val headers = mutableMapOf<String, String>()
                    for ((k, v) in resp.headers) {
                        if (!k.equals("content-encoding", ignoreCase = true) && !k.equals("content-length", ignoreCase = true)) {
                            headers[k] = v
                        }
                    }
                    headers["Content-Type"] = "text/html; charset=utf-8"
                    headers["Access-Control-Allow-Origin"] = "*"
                    return WebResourceResponse("text/html", "utf-8", 200, "OK", headers, java.io.ByteArrayInputStream(bytes))
                }
            } catch (e: Exception) {
                // Fallback to WebView default network
            }
        }

        return null
    }
}
