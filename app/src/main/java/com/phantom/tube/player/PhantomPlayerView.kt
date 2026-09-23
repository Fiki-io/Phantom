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
        /* 1. Endscreen elements & annotations (video cards, playlists, videowall) */
        .ytp-ce-element,
        .ytp-ce-element *,
        .ytp-ce-video,
        .ytp-ce-channel,
        .ytp-ce-playlist,
        .ytp-ce-website,
        .ytp-ce-merchandise,
        .ytp-ce-covering-overlay,
        .ytp-ce-covering-image,
        .ytp-ce-expanding-overlay,
        .ytp-ce-expanding-image,
        .ytp-ce-element-show,
        .ytp-ce-shown,
        .ytp-ce-element-shadow,
        .ytp-ce-hide-button-container,
        .html5-endscreen,
        .html5-endscreen *,
        .html5-ypc-endscreen,
        .html5-ypc-endscreen *,
        .modern-videowall-endscreen,
        .modern-videowall-endscreen *,
        .video-annotations,
        .video-annotations *,
        .video-legacy-annotations,
        .video-legacy-annotations *,
        .ytp-endscreen-content,
        .ytp-endscreen-content *,
        .ytp-endscreen-next,
        .ytp-endscreen-paginate,
        .ytp-endscreen-previous,
        .ytp-endscreen-takeover,
        .ytp-videowall-still,
        .ytp-videowall-still *,
        .iv-card,
        .iv-card *,
        .iv-promo,
        .iv-promo *,
        [class*="ytp-ce"],
        [class*="ytp-ce"] *,
        [class*="endscreen"],
        [class*="endscreen"] *,
        [class*="annotation"],
        [class*="annotation"] *,
        [class*="videowall"],
        [class*="videowall"] *,
        [class*="iv-card"],
        [class*="iv-card"] *,
        [class*="iv-promo"],
        [class*="iv-promo"] * {
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

        /* 2. Play/Pause bezel animation, large play button & center overlays */
        .ytp-bezel,
        .ytp-bezel *,
        .ytp-bezel-icon,
        .ytp-bezel-icon *,
        .ytp-bezel-text,
        .ytp-bezel-text-wrapper,
        .ytp-large-play-button,
        .ytp-large-play-button *,
        .ytp-large-play-button-bg,
        .ytp-play-button,
        .ytp-play-button *,
        button.ytp-play-button,
        .ytp-pause-overlay,
        .ytp-pause-overlay *,
        .ytp-pause-overlay-backdrop,
        .ytp-pause-overlay-container,
        .ytp-cued-thumbnail-overlay,
        .ytp-cued-thumbnail-overlay-image,
        .ytp-spinner,
        .ytp-paid-content-overlay,
        [class*="ytp-bezel"],
        [class*="ytp-bezel"] *,
        [class*="ytp-large-play"],
        [class*="ytp-large-play"] *,
        [class*="ytp-play-button"],
        [class*="ytp-play-button"] *,
        [class*="ytp-pause"],
        [class*="ytp-pause"] * {
            display: none !important;
            opacity: 0 !important;
            visibility: hidden !important;
            pointer-events: none !important;
            animation: none !important;
            -webkit-animation: none !important;
            width: 0 !important;
            height: 0 !important;
            position: absolute !important;
            top: -9999px !important;
            left: -9999px !important;
            z-index: -9999 !important;
        }

        /* 3. Bottom controls, progress bar & gradient */
        .ytp-chrome-bottom,
        .ytp-chrome-bottom *,
        .ytp-progress-bar-container,
        .ytp-progress-bar-container *,
        .ytp-progress-bar,
        .ytp-progress-bar *,
        .ytp-play-progress,
        .ytp-load-progress,
        .ytp-gradient-bottom,
        [class*="ytp-chrome-bottom"],
        [class*="ytp-chrome-bottom"] * {
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

        /* 4. Top bar, titles, channel logo/avatar & share button */
        .ytp-chrome-top,
        .ytp-chrome-top *,
        .ytp-title,
        .ytp-title *,
        .ytp-title-channel,
        .ytp-title-channel *,
        .ytp-title-channel-logo,
        .ytp-title-channel-name,
        span.ytp-title-channel-name,
        a.ytp-title-channel,
        .ytp-title-link,
        .ytp-title-subtext,
        .ytp-title-expanded,
        .ytp-title-show-expanded,
        .ytp-share-button,
        .ytp-share-button *,
        .ytp-share-panel,
        .ytp-share-panel *,
        .ytp-gradient-top,
        .ytp-cards-teaser,
        .ytp-cards-teaser *,
        .ytp-cards-button,
        .ytp-cards-button *,
        [class*="ytp-chrome-top"],
        [class*="ytp-chrome-top"] *,
        [class*="ytp-title"],
        [class*="ytp-title"] *,
        [class*="ytp-share"],
        [class*="ytp-share"] *,
        [class*="cards-teaser"],
        [class*="cards-teaser"] *,
        [class*="channel"],
        [class*="channel"] *,
        [class*="avatar"],
        [class*="avatar"] *,
        a[href*="/@"],
        a[href*="/channel/"],
        a[href*="/user/"],
        a[href*="/c/"],
        img[src*="ggpht.com"],
        img[src*="googleusercontent.com"],
        [style*="ggpht.com"],
        [style*="googleusercontent.com"] {
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

        /* 5. Watermarks, YouTube logo & impression links */
        .ytp-watermark,
        .ytp-watermark *,
        .ytp-youtube-button,
        .ytp-youtube-button *,
        .ytp-impression-link,
        .ytp-impression-link *,
        [class*="ytp-watermark"],
        [class*="ytp-watermark"] *,
        [class*="ytp-youtube"],
        [class*="ytp-youtube"] *,
        [class*="ytp-impression"],
        [class*="ytp-impression"] *,
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

        val isPlayerCss = (url.contains(".css") || url.contains("/ss/")) &&
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
                val styleNonceMatch = Regex("""<style[^>]*nonce=["']([^"']+)["']""").find(rawHtml)
                    ?: Regex("""nonce=["']([^"']+)["']""").find(rawHtml)
                val styleNonce = styleNonceMatch?.groupValues?.get(1) ?: ""
                val styleNonceAttr = if (styleNonce.isNotEmpty()) " nonce=\"$styleNonce\"" else ""

                val scriptNonceMatch = Regex("""<script[^>]*nonce=["']([^"']+)["']""").find(rawHtml)
                    ?: Regex("""nonce=["']([^"']+)["']""").find(rawHtml)
                val scriptNonce = scriptNonceMatch?.groupValues?.get(1) ?: ""
                val scriptNonceAttr = if (scriptNonce.isNotEmpty()) " nonce=\"$scriptNonce\"" else ""

                val styleToInject = "<style id=\"phantom-clean-engine\"$styleNonceAttr>$CSS_RULES</style>"
                val scriptToInject = """
                    <script id="phantom-clean-script"$scriptNonceAttr>
                    (function() {
                        var selectors = [
                            // Endscreen recommendations, videos, channels, playlists, walls & cards
                            '.ytp-ce-element', '.ytp-ce-video', '.ytp-ce-channel', '.ytp-ce-playlist',
                            '.ytp-ce-website', '.ytp-ce-merchandise', '.ytp-ce-covering-overlay',
                            '.ytp-ce-covering-image', '.ytp-ce-expanding-overlay', '.ytp-ce-expanding-image',
                            '.ytp-ce-element-show', '.ytp-ce-shown', '.ytp-ce-element-shadow',
                            '.ytp-ce-hide-button-container', '.ytp-ce-size-medium', '.ytp-ce-size-large',
                            '.ytp-ce-bottom-right-quad', '.ytp-ce-bottom-left-quad', '.ytp-ce-top-right-quad', '.ytp-ce-top-left-quad',
                            '[class*="ytp-ce"]', '.html5-endscreen', '.html5-ypc-endscreen',
                            '.modern-videowall-endscreen', '.ytp-endscreen-content', '.ytp-endscreen-next',
                            '.ytp-endscreen-paginate', '.ytp-endscreen-previous', '.ytp-endscreen-takeover',
                            '.ytp-videowall-still', '[class*="endscreen"]',
                            '.iv-card', '.iv-promo', '[class*="iv-card"]', '[class*="iv-promo"]',
                            '.video-annotations', '.video-legacy-annotations', '[class*="annotation"]', '[class*="videowall"]',

                            // Play, Pause, Bezel animated flash, Center overlays & spinner
                            '.ytp-bezel', '.ytp-bezel-icon', '.ytp-bezel-text', '.ytp-bezel-text-wrapper', '[class*="ytp-bezel"]',
                            '.ytp-large-play-button', '.ytp-large-play-button-bg', '[class*="ytp-large-play"]',
                            '.ytp-play-button', 'button.ytp-play-button', '[class*="ytp-play-button"]',
                            '.ytp-pause-overlay', '.ytp-pause-overlay-backdrop', '.ytp-pause-overlay-container', '[class*="ytp-pause"]',
                            '.ytp-cued-thumbnail-overlay', '.ytp-cued-thumbnail-overlay-image', '.ytp-spinner',
                            'button[aria-label*="Play"]', 'button[aria-label*="Putar"]',
                            'button[aria-label*="Pause"]', 'button[aria-label*="Jeda"]',

                            // Bottom scrubber, progress bar & gradient
                            '.ytp-chrome-bottom', '.ytp-progress-bar-container', '.ytp-progress-bar',
                            '.ytp-play-progress', '.ytp-load-progress', '.ytp-gradient-bottom',
                            '[class*="ytp-chrome-bottom"]',

                            // Top bar, title, channel avatar, logo, share button & panels
                            '.ytp-chrome-top', '.ytp-chrome-top-buttons',
                            '.ytp-title', '.ytp-title-text', '.ytp-title-channel', '.ytp-title-channel-logo',
                            '.ytp-title-channel-name', 'span.ytp-title-channel-name', 'a.ytp-title-channel',
                            '.ytp-title-link', '.ytp-title-subtext', '.ytp-title-expanded', '.ytp-title-show-expanded',
                            '.ytp-share-button', '.ytp-share-panel', '.ytp-share-icon',
                            'button[aria-label*="Share"]', 'button[aria-label*="Bagikan"]', '[class*="ytp-share"]',
                            '.ytp-gradient-top', '.ytp-cards-button', '.ytp-cards-teaser',
                            '.ytp-cards-teaser-channel-avatar', '.ytp-cards-teaser-box', '.ytp-cards-teaser-text', '.ytp-cards-teaser-label',
                            '[class*="ytp-chrome-top"]', '[class*="ytp-title"]', '[class*="cards-teaser"]',
                            '[class*="channel"]', '[class*="avatar"]', '[class*="author"]',
                            'a[href*="/@"]', 'a[href*="/channel/"]', 'a[href*="/user/"]', 'a[href*="/c/"]',
                            'img[src*="ggpht.com"]', 'img[src*="googleusercontent.com"]',
                            '[style*="ggpht.com"]', '[style*="googleusercontent.com"]',

                            // Watermark, YouTube Logo & Watch on YouTube buttons
                            '.ytp-watermark', '.ytp-youtube-button', '.ytp-watch-on-youtube-button',
                            '.ytp-impression-link', '.ytp-impression-link-logo', '.ytp-impression-link-text',
                            '[class*="ytp-watermark"]', '[class*="ytp-youtube"]', '[class*="ytp-impression"]',
                            'a[aria-label*="YouTube"]', 'a[href*="youtube.com/watch"]'
                        ];

                        function clean() {
                            for (var s = 0; s < selectors.length; s++) {
                                try {
                                    var els = document.querySelectorAll(selectors[s]);
                                    for (var i = 0; i < els.length; i++) {
                                        try {
                                            els[i].style.setProperty('display', 'none', 'important');
                                            els[i].style.setProperty('opacity', '0', 'important');
                                            els[i].style.setProperty('visibility', 'hidden', 'important');
                                            els[i].style.setProperty('pointer-events', 'none', 'important');
                                            els[i].remove();
                                        } catch(e) {}
                                    }
                                } catch(e) {}
                            }

                            try {
                                var player = document.querySelector('.html5-video-player');
                                if (player && player.children) {
                                    var kids = Array.prototype.slice.call(player.children);
                                    for (var k = 0; k < kids.length; k++) {
                                        var child = kids[k];
                                        if (child.classList.contains('html5-video-container') ||
                                            child.classList.contains('caption-window') ||
                                            child.classList.contains('ytp-caption-window-container') ||
                                            child.tagName.toLowerCase() === 'video') {
                                            continue;
                                        }
                                        try {
                                            child.style.setProperty('display', 'none', 'important');
                                            child.style.setProperty('opacity', '0', 'important');
                                            child.style.setProperty('visibility', 'hidden', 'important');
                                            child.style.setProperty('pointer-events', 'none', 'important');
                                            child.remove();
                                        } catch(e) {}
                                    }
                                }
                            } catch(e) {}
                        }

                        clean();
                        var obs = new MutationObserver(clean);
                        if (document.documentElement) {
                            obs.observe(document.documentElement, {
                                childList: true,
                                subtree: true
                            });
                        }
                        document.addEventListener('DOMContentLoaded', clean);
                        window.addEventListener('load', clean);
                        setInterval(clean, 50);
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
