package com.phantom.tube.data.innertube.parser

import org.json.JSONObject

object InnerTubeHelpers {

    fun parseRunsText(obj: JSONObject?): String {
        if (obj == null) return ""
        val content = obj.optString("content")
        if (content.isNotBlank()) return content
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

    /**
     * Standardizes raw YouTube view count strings so that Home, Search, Channel,
     * and Related video feeds display uniform formatting (e.g. "98 jt x ditonton" or "10 rb x ditonton").
     */
    fun normalizeViewCount(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val lower = trimmed.lowercase()
        // If already formatted with view indicator or live status, leave as is
        if (lower.contains("ditonton") || lower.contains("view") || lower.contains("live") || lower.contains("streaming")) {
            return trimmed
        }
        // If it's a short count like "98 jt", "10 rb", "1,2 jt", "500K", or digits
        return "$trimmed x ditonton"
    }

    /**
     * Standardizes raw YouTube relative time strings so that Home, Search, Channel,
     * and Related video feeds display uniform formatting (e.g. "4 tahun lalu" or "2 minggu lalu").
     */
    fun normalizePublishedTime(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val lower = trimmed.lowercase()
        // If already formatted with relative time indicator
        if (lower.contains("lalu") || lower.contains("ago") || lower.contains("kemarin") || lower.contains("hari ini")) {
            return trimmed
        }
        // Indonesian time units without "lalu" (common in modern lockupViewModel)
        val indonesianUnits = listOf("tahun", "thn", "bulan", "bln", "minggu", "mgg", "hari", "hr", "jam", "menit", "mnt", "detik", "dtk")
        if (indonesianUnits.any { lower.contains(it) }) {
            return "$trimmed lalu"
        }
        // English time units without "ago"
        val englishUnits = listOf("year", "years", "month", "months", "week", "weeks", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds")
        if (englishUnits.any { lower.contains(it) }) {
            return "$trimmed ago"
        }
        return trimmed
    }

    /**
     * Centralized metadata line builder for video cards and details across the entire app.
     * Guarantees consistent styling: "Channel • 98 jt x ditonton • 4 tahun lalu"
     */
    fun formatVideoMeta(channelTitle: String, viewCountText: String, publishedTimeText: String): String {
        return buildString {
            if (channelTitle.isNotBlank()) {
                append(channelTitle)
            }
            val normViews = normalizeViewCount(viewCountText)
            if (normViews.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(normViews)
            }
            val normDate = normalizePublishedTime(publishedTimeText)
            if (normDate.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(normDate)
            }
        }
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
