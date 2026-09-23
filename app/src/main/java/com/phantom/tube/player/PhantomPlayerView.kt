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
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = false
                    allowContentAccess = false
                    offscreenPreRaster = true

                    // Clean Chrome User-Agent
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }

                clearCache(true)
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
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val cssCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()

    val CSS_RULES = """
        .ytp-chrome-top,
        .ytp-chrome-top-buttons,
        .ytp-chrome-bottom,
        .ytp-chrome-controls,
        .ytp-title,
        .ytp-title-text,
        .ytp-title-channel,
        .ytp-title-channel-logo,
        .ytp-title-channel-name,
        .ytp-title-link,
        .ytp-title-subtext,
        .ytp-title-expanded,
        .ytp-show-cards-title,
        .ytp-cards-button,
        .ytp-cards-button-title,
        .ytp-cards-teaser,
        .ytp-share-button,
        .ytp-share-button-visible,
        .ytp-share-icon,
        .ytp-share-panel,
        .ytp-share-title,
        .ytp-overflow-button,
        a.ytp-title-link,
        .ytp-watermark,
        .ytp-watermark-small,
        .ytp-muted-autoplay-watermark,
        .ytp-youtube-button,
        .ytp-youtube-music-button,
        .ytp-watch-on-youtube-button,
        a.ytp-youtube-button,
        .ytp-impression-link,
        .ytp-impression-link-logo,
        .ytp-impression-link-text,
        .ytp-music-impression-link,
        a.ytp-impression-link,
        .ytp-gradient-top,
        .ytp-gradient-bottom,
        .ytp-large-play-button,
        .ytp-large-play-button-bg,
        .ytp-large-play-button-red-bg,
        .ytp-dni-large-play-button-bg,
        .ytp-play-button,
        .ytp-pause-overlay,
        .ytp-pause-overlay-backdrop,
        .ytp-pause-overlay-container,
        .ytp-endscreen-content,
        .ytp-autonav-endscreen-countdown-container,
        .ytp-autonav-endscreen-countdown-overlay,
        .ytp-modern-endscreen-content,
        .ytp-ce-element,
        .ytp-bezel,
        .ytp-bezel-text,
        .ytp-bezel-icon,
        .ytp-bezel-text-wrapper,
        .ytp-doubletap-ui,
        .ytp-cued-thumbnail-overlay,
        .ytp-cued-thumbnail-overlay-image,
        .ytp-spinner,
        .ytp-contextmenu,
        .ytp-paid-content-overlay,
        .ytp-offline-slate,
        .ytp-suggested-action-badge,
        .ytp-more-videos-button,
        .ytp-progress-bar-container,
        .ytp-progress-bar,
        .ytp-play-progress,
        .ytp-load-progress,
        .attribution-button,
        .iv-drawer,
        .iv-card,
        [class*="ytp-chrome"],
        [class*="ytp-title"],
        [class*="ytp-share"],
        [class*="ytp-watermark"],
        [class*="ytp-impression"],
        [class*="ytp-large-play"],
        [class*="ytp-bezel"],
        [class*="ytp-pause"],
        [class*="ytp-gradient"],
        [class*="ytp-endscreen"],
        [class*="ytp-youtube"],
        [class*="ytp-cards"],
        button[aria-label*="Play" i],
        button[aria-label*="Share" i],
        button[aria-label*="Putar" i],
        button[aria-label*="Bagikan" i],
        a[aria-label*="YouTube" i],
        a[title*="YouTube" i],
        a[href*="youtube.com/watch"] {
            display: none !important;
            opacity: 0 !important;
            visibility: hidden !important;
            pointer-events: none !important;
            width: 0 !important;
            height: 0 !important;
            position: absolute !important;
            top: -9999px !important;
            left: -9999px !important;
            z-index: -9999 !important;
        }
    """.trimIndent()

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        if (request.method != "GET") return null

        val isEmbedHtml = (url.contains("/embed/") || url.contains("/embed?")) &&
                (url.contains("youtube.com") || url.contains("youtube-nocookie.com"))

        val isPlayerCss = url.contains(".css") &&
                (url.contains("youtube.com") || url.contains("googlevideo.com") || url.contains("youtube-nocookie.com"))

        if (!isEmbedHtml && !isPlayerCss) return null

        try {
            if (isPlayerCss) {
                val cached = cssCache[url]
                if (cached != null) {
                    val headers = mapOf(
                        "Content-Type" to "text/css; charset=utf-8",
                        "Access-Control-Allow-Origin" to "*"
                    )
                    return WebResourceResponse("text/css", "utf-8", 200, "OK", headers, java.io.ByteArrayInputStream(cached))
                }
            }

            val reqBuilder = okhttp3.Request.Builder().url(url)
            for ((k, v) in request.requestHeaders) {
                // Strip Accept-Encoding so OkHttp decompresses gzip transparently
                if (!k.equals("accept-encoding", ignoreCase = true)) {
                    reqBuilder.header(k, v)
                }
            }

            val resp = httpClient.newCall(reqBuilder.build()).execute()
            if (!resp.isSuccessful) return null

            if (isPlayerCss) {
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

            if (isEmbedHtml) {
                val rawHtml = resp.body?.string() ?: ""
                val nonceMatch = Regex("""nonce=["']([^"']+)["']""").find(rawHtml)
                val nonceAttr = if (nonceMatch != null) " nonce=\"${nonceMatch.groupValues[1]}\"" else ""
                val styleToInject = "<style id=\"phantom-clean-engine\"$nonceAttr>$CSS_RULES</style>"
                val scriptToInject = """
                    <script id="phantom-clean-script"$nonceAttr>
                    (function() {
                        var selectors = [
                            '.ytp-chrome-top', '.ytp-chrome-top-buttons', '.ytp-chrome-bottom',
                            '.ytp-watermark', '.ytp-youtube-button', '.ytp-watch-on-youtube-button',
                            '.ytp-impression-link', '.ytp-large-play-button', '.ytp-large-play-button-bg',
                            '.ytp-pause-overlay', '.ytp-share-button', '.ytp-share-panel', '.ytp-bezel',
                            '.ytp-title', '.ytp-title-text', '.ytp-title-channel', '.ytp-gradient-top',
                            '.ytp-gradient-bottom', '.ytp-cards-button', '.ytp-contextmenu',
                            '.ytp-cued-thumbnail-overlay', '.ytp-show-cards-title', '.ytp-spinner',
                            '.ytp-progress-bar-container', '.ytp-progress-bar',
                            'a.ytp-title-link', 'a.ytp-youtube-button', 'a[aria-label*="YouTube"]',
                            'button[aria-label*="Play"]', 'button[aria-label*="Share"]'
                        ];
                        function clean() {
                            selectors.forEach(function(sel) {
                                try {
                                    var els = document.querySelectorAll(sel);
                                    for (var i = 0; i < els.length; i++) {
                                        els[i].style.setProperty('display', 'none', 'important');
                                        els[i].style.setProperty('opacity', '0', 'important');
                                        els[i].style.setProperty('visibility', 'hidden', 'important');
                                        els[i].style.setProperty('pointer-events', 'none', 'important');
                                        els[i].style.setProperty('width', '0', 'important');
                                        els[i].style.setProperty('height', '0', 'important');
                                    }
                                } catch(e) {}
                            });
                        }
                        clean();
                        var obs = new MutationObserver(clean);
                        if (document.documentElement) {
                            obs.observe(document.documentElement, { childList: true, subtree: true });
                        }
                        document.addEventListener('DOMContentLoaded', clean);
                        window.addEventListener('load', clean);
                        setInterval(clean, 250);
                    })();
                    </script>
                """.trimIndent()

                val payload = styleToInject + scriptToInject
                val modifiedHtml = when {
                    rawHtml.contains("</head>") -> rawHtml.replace("</head>", "$payload</head>")
                    rawHtml.contains("<body") -> rawHtml.replace("<body", "$payload<body")
                    else -> payload + rawHtml
                }

                val bytes = modifiedHtml.toByteArray(Charsets.UTF_8)
                val headers = mutableMapOf<String, String>()
                for ((k, v) in resp.headers) {
                    val lower = k.lowercase()
                    // Strip CSP and encoding headers so injected styles and scripts run unhindered
                    if (lower != "content-encoding" &&
                        lower != "content-length" &&
                        !lower.contains("content-security-policy") &&
                        !lower.contains("x-content-security-policy") &&
                        !lower.contains("x-webkit-csp")
                    ) {
                        headers[k] = v
                    }
                }
                headers["Content-Type"] = "text/html; charset=utf-8"
                headers["Access-Control-Allow-Origin"] = "*"
                return WebResourceResponse("text/html", "utf-8", 200, "OK", headers, java.io.ByteArrayInputStream(bytes))
            }
        } catch (e: Exception) {
            android.util.Log.e("PhantomCleanEngine", "Intercept error for $url", e)
        }

        return null
    }
}
