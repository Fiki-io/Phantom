#include <jni.h>
#include <string>

namespace {

const char* RAW_ENGINE_HTML = R"HTML(
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Phantom Engine</title>
    <style>
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            background-color: #000000;
            overflow: hidden;
            -webkit-user-select: none;
            user-select: none;
            -webkit-tap-highlight-color: transparent;
        }
        #player {
            width: 100%;
            height: 100%;
            pointer-events: none;
            transform: translateZ(0);
            will-change: transform;
            backface-visibility: hidden;
        }
        iframe {
            width: 100% !important;
            height: 100% !important;
            border: 0 !important;
            outline: 0 !important;
            transform: translateZ(0);
        }
    </style>
    <script>
        try {
            Object.defineProperty(document, 'hidden', {
                get: function() { return false; },
                configurable: true
            });
            Object.defineProperty(document, 'visibilityState', {
                get: function() { return 'visible'; },
                configurable: true
            });
            Object.defineProperty(document, 'webkitVisibilityState', {
                get: function() { return 'visible'; },
                configurable: true
            });
            Object.defineProperty(document, 'webkitHidden', {
                get: function() { return false; },
                configurable: true
            });
        } catch(e) {}

        window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
        document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
        window.addEventListener('webkitvisibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
        document.addEventListener('webkitvisibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
        window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);
        window.addEventListener('pagehide', function(e) { e.stopImmediatePropagation(); }, true);
        window.addEventListener('freeze', function(e) { e.stopImmediatePropagation(); }, true);
    </script>
</head>
<body>
    <div id="player"></div>

    <script>
        var player = null;
        var timeTicker = null;

        var urlParams = new URLSearchParams(window.location.search);
        var initialVideoId = urlParams.get('v') || '';
        var initialStartSeconds = parseFloat(urlParams.get('start') || '0');
        var activeVideoId = initialVideoId;

        var tag = document.createElement('script');
        tag.src = "https://www.youtube.com/iframe_api";
        tag.onerror = function() {
            if (window.PhantomBridge) {
                window.PhantomBridge.onError(999);
            }
        };
        var firstScriptTag = document.getElementsByTagName('script')[0];
        firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

        function onYouTubeIframeAPIReady() {
            var playerConfig = {
                height: '100%',
                width: '100%',
                playerVars: {
                    'autoplay': 1,
                    'controls': 0,
                    'disablekb': 1,
                    'fs': 0,
                    'modestbranding': 1,
                    'rel': 0,
                    'iv_load_policy': 3,
                    'playsinline': 1,
                    'enablejsapi': 1,
                    'showinfo': 0,
                    'autohide': 1,
                    'cc_load_policy': 0,
                    'origin': 'https://appassets.androidplatform.net',
                    'widget_referrer': 'https://appassets.androidplatform.net'
                },
                events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange,
                    'onError': onPlayerError
                }
            };

            if (initialVideoId && initialVideoId.length > 2) {
                playerConfig.videoId = initialVideoId;
                if (initialStartSeconds > 0) {
                    playerConfig.playerVars.start = Math.floor(initialStartSeconds);
                }
            }

            player = new YT.Player('player', playerConfig);
        }

        function onPlayerReady(event) {
            if (window.PhantomBridge) {
                window.PhantomBridge.onReady();
            }
            startTimeTicker();
            if (event.target && typeof event.target.playVideo === 'function') {
                event.target.playVideo();
            }
        }

        function onPlayerStateChange(event) {
            if (event.data === 1 || event.data === 3) {
                startTimeTicker();
                try {
                    if (player && typeof player.getAvailableQualityLevels === 'function') {
                        var lvls = player.getAvailableQualityLevels() || [];
                        var cur = (typeof player.getPlaybackQuality === 'function') ? player.getPlaybackQuality() : 'auto';
                        if (window.PhantomBridge && typeof window.PhantomBridge.onQualityChange === 'function') {
                            window.PhantomBridge.onQualityChange(cur || 'auto', JSON.stringify(lvls));
                        }
                    }
                } catch(e) {}
            } else {
                stopTimeTicker();
            }
            if (window.PhantomBridge) {
                window.PhantomBridge.onStateChange(event.data);
            }
        }

        function onPlayerError(event) {
            stopTimeTicker();
            if (window.PhantomBridge) {
                window.PhantomBridge.onError(event.data);
            }
        }

        function stopTimeTicker() {
            if (timeTicker) {
                clearInterval(timeTicker);
                timeTicker = null;
            }
        }

        function startTimeTicker() {
            if (timeTicker) clearInterval(timeTicker);
            timeTicker = setInterval(function() {
                try {
                    if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                        var videoData = (typeof player.getVideoData === 'function') ? player.getVideoData() : null;
                        var loadedId = (videoData && videoData.video_id) ? videoData.video_id : activeVideoId;
                        if (activeVideoId && loadedId && loadedId !== activeVideoId) {
                            return;
                        }
                        var current = player.getCurrentTime() || 0;
                        var duration = player.getDuration() || 0;
                        var loaded = (typeof player.getVideoLoadedFraction === 'function' ? player.getVideoLoadedFraction() : 0) || 0;
                        if (window.PhantomBridge) {
                            window.PhantomBridge.onTimeUpdate(activeVideoId, current, duration, loaded);
                        }
                    }
                } catch(e) {}
            }, 250);
        }

        window.addEventListener('message', function(e) {
            if (e.data && e.data.type === 'PHANTOM_QUALITY_REPORT') {
                if (window.PhantomBridge && typeof window.PhantomBridge.onQualityChange === 'function') {
                    window.PhantomBridge.onQualityChange(e.data.current || 'auto', JSON.stringify(e.data.levels || []));
                }
            }
        });

        window.loadVideo = function(videoId, startSec) {
            try {
                activeVideoId = videoId;
                stopTimeTicker();
                if (player && typeof player.loadVideoById === 'function') {
                    player.loadVideoById({
                        'videoId': videoId,
                        'startSeconds': startSec || 0
                    });
                }
            } catch(e) {
                console.error("loadVideo error: " + e);
            }
        };

        window.playVideo = function() {
            try {
                if (player && typeof player.playVideo === 'function') player.playVideo();
            } catch(e) {}
        };

        window.pauseVideo = function() {
            try {
                stopTimeTicker();
                if (player && typeof player.pauseVideo === 'function') player.pauseVideo();
            } catch(e) {}
        };

        window.seekTo = function(seconds) {
            try {
                if (player && typeof player.seekTo === 'function') player.seekTo(seconds, true);
            } catch(e) {}
        };

        window.setPlaybackRate = function(rate) {
            try {
                if (player && typeof player.setPlaybackRate === 'function') player.setPlaybackRate(rate);
            } catch(e) {}
        };

        window.setLoop = function(loop) {
            try {
                if (player && typeof player.setLoop === 'function') {
                    player.setLoop(loop);
                }
            } catch(e) {}
        };

        window.setPlaybackQuality = function(quality) {
            try {
                if (player) {
                    if (typeof player.setPlaybackQualityRange === 'function') {
                        player.setPlaybackQualityRange(quality, quality);
                    }
                    if (typeof player.setPlaybackQuality === 'function') {
                        player.setPlaybackQuality(quality);
                    }
                }
                var iframe = document.querySelector('iframe');
                if (iframe && iframe.contentWindow) {
                    iframe.contentWindow.postMessage({ type: 'PHANTOM_SET_QUALITY', quality: quality }, '*');
                }
            } catch(e) {}
        };
    </script>
</body>
</html>
)HTML";

const char* RAW_CSS_RULES = R"CSS(
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
)CSS";

const char* RAW_CLEAN_SCRIPT = R"JS(
(function() {
    var selectors = [
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

        '.ytp-bezel', '.ytp-bezel-icon', '.ytp-bezel-text', '.ytp-bezel-text-wrapper', '[class*="ytp-bezel"]',
        '.ytp-large-play-button', '.ytp-large-play-button-bg', '[class*="ytp-large-play"]',
        '.ytp-play-button', 'button.ytp-play-button', '[class*="ytp-play-button"]',
        '.ytp-pause-overlay', '.ytp-pause-overlay-backdrop', '.ytp-pause-overlay-container', '[class*="ytp-pause"]',
        '.ytp-cued-thumbnail-overlay', '.ytp-cued-thumbnail-overlay-image', '.ytp-spinner',
        'button[aria-label*="Play"]', 'button[aria-label*="Putar"]',
        'button[aria-label*="Pause"]', 'button[aria-label*="Jeda"]',

        '.ytp-chrome-bottom', '.ytp-progress-bar-container', '.ytp-progress-bar',
        '.ytp-play-progress', '.ytp-load-progress', '.ytp-gradient-bottom',
        '[class*="ytp-chrome-bottom"]',

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

    window.addEventListener('message', function(e) {
        if (e.data && e.data.type === 'PHANTOM_SET_QUALITY') {
            var q = e.data.quality;
            var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
            if (p) {
                try { if (typeof p.setPlaybackQualityRange === 'function') p.setPlaybackQualityRange(q, q); } catch(err) {}
                try { if (typeof p.setPlaybackQuality === 'function') p.setPlaybackQuality(q); } catch(err) {}
            }
        }
    });

    function reportIframeQuality() {
        try {
            var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
            if (p) {
                var levels = (typeof p.getAvailableQualityLevels === 'function') ? p.getAvailableQualityLevels() : [];
                var current = (typeof p.getPlaybackQuality === 'function') ? p.getPlaybackQuality() : 'auto';
                if (levels && levels.length > 0) {
                    window.parent.postMessage({
                        type: 'PHANTOM_QUALITY_REPORT',
                        current: current,
                        levels: levels
                    }, '*');
                }
            }
        } catch(e) {}
    }
    setInterval(reportIframeQuality, 1500);
    document.addEventListener('DOMContentLoaded', reportIframeQuality);
    window.addEventListener('load', reportIframeQuality);
})();
)JS";

} // namespace

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getEngineHtml(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(RAW_ENGINE_HTML);
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getCleanCssRules(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(RAW_CSS_RULES);
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getCleanScript(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(RAW_CLEAN_SCRIPT);
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getVirtualOrigin(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF("https://appassets.androidplatform.net");
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getWebUserAgent(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    );
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getInnerTubeDesktopUserAgent(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    );
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getSponsorBlockUrl(
        JNIEnv* env,
        jobject /* this */,
        jstring videoId) {
    const char* vidChars = env->GetStringUTFChars(videoId, nullptr);
    std::string vidStr = vidChars ? vidChars : "";
    if (vidChars) {
        env->ReleaseStringUTFChars(videoId, vidChars);
    }

    std::string url = "https://sponsor.ajay.app/api/skipSegments?videoID=" + vidStr +
        "&categories=%5B%22sponsor%22%2C%22selfpromo%22%2C%22interaction%22%2C%22intro%22%2C%22outro%22%5D";

    return env->NewStringUTF(url.c_str());
}

/**
 * Classifies the incoming network request URL.
 * Returns:
 *   1 -> In-memory Virtual Engine HTML route ("/engine/render")
 *   2 -> Embed HTML target to inject CSS/cleaner
 *   3 -> Video player CSS target to patch
 *   0 -> Pass-through / no modification
 */
JNIEXPORT jint JNICALL
Java_com_phantom_tube_core_security_PhantomNative_classifyUrl(
        JNIEnv* env,
        jobject /* this */,
        jstring urlStr) {
    const char* chars = env->GetStringUTFChars(urlStr, nullptr);
    if (!chars) return 0;
    std::string url = chars;
    env->ReleaseStringUTFChars(urlStr, chars);

    if (url.find("/engine/render") != std::string::npos) {
        return 1;
    }

    bool isYtHost = (url.find("youtube.com") != std::string::npos) ||
                    (url.find("youtube-nocookie.com") != std::string::npos);

    if (isYtHost && (url.find("/embed/") != std::string::npos || url.find("/embed?") != std::string::npos)) {
        return 2;
    }

    bool isMediaHost = isYtHost || (url.find("googlevideo.com") != std::string::npos);
    if (isMediaHost && (url.find(".css") != std::string::npos || url.find("/ss/") != std::string::npos)) {
        return 3;
    }

    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getSearchUrl(
        JNIEnv* env, jobject /* this */) {
    return env->NewStringUTF("https://www.youtube.com/youtubei/v1/search?prettyPrint=false");
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getNextUrl(
        JNIEnv* env, jobject /* this */) {
    return env->NewStringUTF("https://www.youtube.com/youtubei/v1/next?prettyPrint=false");
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getBrowseUrl(
        JNIEnv* env, jobject /* this */) {
    return env->NewStringUTF("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false");
}

JNIEXPORT jstring JNICALL
Java_com_phantom_tube_core_security_PhantomNative_getSuggestUrl(
        JNIEnv* env, jobject /* this */, jint index, jstring queryStr) {
    const char* chars = env->GetStringUTFChars(queryStr, nullptr);
    std::string q = chars ? chars : "";
    if (chars) env->ReleaseStringUTFChars(queryStr, chars);

    if (index == 0) {
        std::string u = "https://suggestqueries-clients6.youtube.com/complete/search?client=firefox&ds=yt&hl=id&gl=ID&q=" + q;
        return env->NewStringUTF(u.c_str());
    } else if (index == 1) {
        std::string u = "https://suggestqueries-clients6.youtube.com/complete/search?client=youtube&ds=yt&hl=id&gl=ID&q=" + q;
        return env->NewStringUTF(u.c_str());
    } else {
        std::string u = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=id&gl=ID&q=" + q;
        return env->NewStringUTF(u.c_str());
    }
}

} // extern "C"
