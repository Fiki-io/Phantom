package com.phantom.tube.core.security

import android.content.Context
import android.util.Log

/**
 * JNI Native Bridge to libphantom_core.so.
 *
 * Shielding mechanism:
 * In compiled release APKs, the bytecode for these methods is native and stored inside the compiled
 * ELF binary (.so), which prevents Dalvik/ART decompilers (JADX, Apktool) and AI crawlers from
 * inspecting or extracting core YouTube cleaning rules, injection scripts, and network signatures.
 */
object PhantomNative {

    private const val TAG = "PhantomNative"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("phantom_core")
            isNativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'phantom_core' not loaded; using fallback: ${e.message}")
            isNativeLoaded = false
        } catch (e: Exception) {
            Log.w(TAG, "Exception loading native library: ${e.message}")
            isNativeLoaded = false
        }
    }

    // --- Native JNI declarations ---
    private external fun getEngineHtml(): String
    private external fun getCleanCssRules(): String
    private external fun getCleanScript(): String
    private external fun getVirtualOrigin(): String
    private external fun getWebUserAgent(): String
    private external fun getInnerTubeDesktopUserAgent(): String
    private external fun getSponsorBlockUrl(videoId: String): String
    private external fun classifyUrl(url: String): Int
    private external fun getSearchUrl(): String
    private external fun getNextUrl(): String
    private external fun getBrowseUrl(): String
    private external fun getSuggestUrl(index: Int, query: String, hl: String, gl: String): String
    private external fun getUpdateUrl(): String
    private external fun verifyAppSecurity(context: Context): Boolean
    private external fun applyVersionControl(currentCode: Int, minCode: Int, forceUpdate: Boolean): Boolean
    private external fun isEngineLocked(): Boolean

    // --- Safe public accessors with host/test fallback ---

    fun verifySecurity(context: Context): Boolean {
        return if (isNativeLoaded) {
            try {
                verifyAppSecurity(context)
            } catch (e: UnsatisfiedLinkError) {
                true
            }
        } else {
            true
        }
    }

    fun applyVersionPolicy(currentCode: Int, minCode: Int, forceUpdate: Boolean): Boolean {
        return if (isNativeLoaded) {
            try {
                applyVersionControl(currentCode, minCode, forceUpdate)
            } catch (e: UnsatisfiedLinkError) {
                true
            }
        } else {
            true
        }
    }

    fun isLocked(): Boolean {
        return if (isNativeLoaded) {
            try {
                isEngineLocked()
            } catch (e: UnsatisfiedLinkError) {
                false
            }
        } else {
            false
        }
    }

    fun getHtml(): String {
        return if (isNativeLoaded) {
            try {
                getEngineHtml()
            } catch (e: UnsatisfiedLinkError) {
                FALLBACK_ENGINE_HTML
            }
        } else {
            FALLBACK_ENGINE_HTML
        }
    }

    fun getCssRules(): String {
        return if (isNativeLoaded) {
            try {
                getCleanCssRules()
            } catch (e: UnsatisfiedLinkError) {
                FALLBACK_CSS_RULES
            }
        } else {
            FALLBACK_CSS_RULES
        }
    }

    fun getCleanEngineScript(): String {
        return if (isNativeLoaded) {
            try {
                getCleanScript()
            } catch (e: UnsatisfiedLinkError) {
                FALLBACK_CLEAN_SCRIPT
            }
        } else {
            FALLBACK_CLEAN_SCRIPT
        }
    }

    fun getOrigin(): String {
        return if (isNativeLoaded) {
            try {
                getVirtualOrigin()
            } catch (e: UnsatisfiedLinkError) {
                "https://appassets.androidplatform.net"
            }
        } else {
            "https://appassets.androidplatform.net"
        }
    }

    fun getUserAgent(): String {
        return if (isNativeLoaded) {
            try {
                getWebUserAgent()
            } catch (e: UnsatisfiedLinkError) {
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
            }
        } else {
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
        }
    }

    fun getDesktopUserAgent(): String {
        return if (isNativeLoaded) {
            try {
                getInnerTubeDesktopUserAgent()
            } catch (e: UnsatisfiedLinkError) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            }
        } else {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }
    }

    fun getSponsorBlockRequestUrl(videoId: String): String {
        return if (isNativeLoaded) {
            try {
                getSponsorBlockUrl(videoId)
            } catch (e: UnsatisfiedLinkError) {
                "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=%5B%22sponsor%22%2C%22selfpromo%22%2C%22interaction%22%2C%22intro%22%2C%22outro%22%5D"
            }
        } else {
            "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=%5B%22sponsor%22%2C%22selfpromo%22%2C%22interaction%22%2C%22intro%22%2C%22outro%22%5D"
        }
    }

    private val YT_BASE by lazy { "https://www." + "you" + "tube" + ".com/youtubei/v1/" }
    private val YT_SUGGEST by lazy { "https://suggestqueries-clients6." + "you" + "tube" + ".com/complete/search" }

    fun getSearchEndpoint(): String {
        return if (isNativeLoaded) {
            try {
                getSearchUrl()
            } catch (e: UnsatisfiedLinkError) {
                "${YT_BASE}search?prettyPrint=false"
            }
        } else {
            "${YT_BASE}search?prettyPrint=false"
        }
    }

    fun getNextEndpoint(): String {
        return if (isNativeLoaded) {
            try {
                getNextUrl()
            } catch (e: UnsatisfiedLinkError) {
                "${YT_BASE}next?prettyPrint=false"
            }
        } else {
            "${YT_BASE}next?prettyPrint=false"
        }
    }

    fun getBrowseEndpoint(): String {
        return if (isNativeLoaded) {
            try {
                getBrowseUrl()
            } catch (e: UnsatisfiedLinkError) {
                "${YT_BASE}browse?prettyPrint=false"
            }
        } else {
            "${YT_BASE}browse?prettyPrint=false"
        }
    }

    fun getSuggestEndpoints(encodedQuery: String, hl: String = "id", gl: String = "ID"): List<String> {
        return if (isNativeLoaded) {
            try {
                listOf(
                    getSuggestUrl(0, encodedQuery, hl, gl),
                    getSuggestUrl(1, encodedQuery, hl, gl),
                    getSuggestUrl(2, encodedQuery, hl, gl)
                )
            } catch (e: UnsatisfiedLinkError) {
                listOf(
                    "${YT_SUGGEST}?client=youtube&ds=yt&hl=$hl&gl=$gl&q=$encodedQuery",
                    "${YT_SUGGEST}?client=firefox&ds=yt&hl=$hl&gl=$gl&q=$encodedQuery",
                    "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=$hl&gl=$gl&q=$encodedQuery"
                )
            }
        } else {
            listOf(
                "${YT_SUGGEST}?client=youtube&ds=yt&hl=$hl&gl=$gl&q=$encodedQuery",
                "${YT_SUGGEST}?client=firefox&ds=yt&hl=$hl&gl=$gl&q=$encodedQuery",
                "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=$hl&gl=$gl&q=$encodedQuery"
            )
        }
    }

    fun classifyRequestUrl(url: String): Int {
        return if (isNativeLoaded) {
            try {
                classifyUrl(url)
            } catch (e: UnsatisfiedLinkError) {
                fallbackClassify(url)
            }
        } else {
            fallbackClassify(url)
        }
    }

    private fun fallbackClassify(url: String): Int {
        if (url.contains("/engine/render")) return 1
        val host1 = "you" + "tube" + ".com"
        val host2 = "you" + "tube" + "-nocookie" + ".com"
        val path1 = "/" + "em" + "bed" + "/"
        val path2 = "/" + "em" + "bed" + "?"
        val isYt = url.contains(host1) || url.contains(host2)
        if (isYt && (url.contains(path1) || url.contains(path2))) return 2
        val host3 = "google" + "video" + ".com"
        val isMedia = isYt || url.contains(host3)
        if (isMedia && (url.contains(".css") || url.contains("/ss/"))) return 3
        return 0
    }

    fun getUpdateEndpoint(): String {
        return if (isNativeLoaded) {
            try {
                getUpdateUrl()
            } catch (e: UnsatisfiedLinkError) {
                FALLBACK_UPDATE_URL
            }
        } else {
            FALLBACK_UPDATE_URL
        }
    }

    // Development/Unit-test fallback stubs
    private const val FALLBACK_ENGINE_HTML = "<!DOCTYPE html><html><body><div id=\"render_surface\"></div></body></html>"
    private const val FALLBACK_CSS_RULES = "body { margin: 0; }"
    private const val FALLBACK_CLEAN_SCRIPT = "(function(){})();"
    private const val FALLBACK_UPDATE_URL = "https://raw.githubusercontent.com/Fiki-io/phantom-version/main/version.json"
}
