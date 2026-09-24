package com.phantom.tube.data.innertube.parser

import org.json.JSONObject

object InnerTubeHelpers {

    fun parseRunsText(obj: JSONObject?): String {
        if (obj == null) return ""
        val simple = obj.optString("simpleText")
        if (simple.isNotBlank()) return simple
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            val r = runs.optJSONObject(i) ?: continue
            sb.append(r.optString("text", ""))
        }
        return sb.toString()
    }

    fun extractThumbnail(obj: JSONObject?, videoId: String): String {
        if (obj == null) return if (videoId.isNotBlank()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else ""
        val thumbnails = obj.optJSONArray("thumbnails")
        if (thumbnails != null && thumbnails.length() > 0) {
            val last = thumbnails.optJSONObject(thumbnails.length() - 1)
            val url = last?.optString("url") ?: ""
            if (url.isNotBlank()) {
                return if (url.startsWith("//")) "https:$url" else url
            }
        }
        return if (videoId.isNotBlank()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else ""
    }

    fun extractAvatar(json: JSONObject): String {
        // 1. channelThumbnailSupportedRenderers -> channelThumbnailWithLinkRenderer -> thumbnail -> thumbnails
        val ctsr = json.optJSONObject("channelThumbnailSupportedRenderers")
            ?.optJSONObject("channelThumbnailWithLinkRenderer")
            ?.optJSONObject("thumbnail")
        val url1 = extractThumbnail(ctsr, "")
        if (url1.isNotBlank()) return url1

        // 2. avatar -> decoratedAvatarViewModel -> avatar -> avatarViewModel -> image -> sources
        val avatarSources = json.optJSONObject("avatar")
            ?.optJSONObject("decoratedAvatarViewModel")
            ?.optJSONObject("avatar")
            ?.optJSONObject("avatarViewModel")
            ?.optJSONObject("image")
            ?.optJSONArray("sources")
        if (avatarSources != null && avatarSources.length() > 0) {
            val last = avatarSources.optJSONObject(avatarSources.length() - 1)
            val url2 = last?.optString("url") ?: ""
            if (url2.isNotBlank()) {
                return if (url2.startsWith("//")) "https:$url2" else url2
            }
        }

        // 3. channelThumbnail -> thumbnails
        val ct = json.optJSONObject("channelThumbnail")
        val url3 = extractThumbnail(ct, "")
        if (url3.isNotBlank()) return url3

        // 4. lockupViewModel metadata image
        val lvmImage = json.optJSONObject("metadata")
            ?.optJSONObject("lockupMetadataViewModel")
            ?.optJSONObject("image")
            ?.optJSONObject("decoratedAvatarViewModel")
            ?.optJSONObject("avatar")
            ?.optJSONObject("avatarViewModel")
            ?.optJSONObject("image")
            ?.optJSONArray("sources")
        if (lvmImage != null && lvmImage.length() > 0) {
            val last = lvmImage.optJSONObject(lvmImage.length() - 1)
            val url4 = last?.optString("url") ?: ""
            if (url4.isNotBlank()) {
                return if (url4.startsWith("//")) "https:$url4" else url4
            }
        }

        return ""
    }
}
