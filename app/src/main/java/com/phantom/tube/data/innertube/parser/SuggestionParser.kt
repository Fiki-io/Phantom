package com.phantom.tube.data.innertube.parser

import com.phantom.tube.data.model.SuggestionItem
import org.json.JSONArray

object SuggestionParser {

    fun parseSuggestions(jsonString: String): List<String> {
        return parseDetailedSuggestions(jsonString).map { it.query }
    }

    private val ucRegex = Regex("UC[a-zA-Z0-9_-]{22}")

    fun parseDetailedSuggestions(jsonString: String): List<SuggestionItem> {
        val suggestions = mutableListOf<SuggestionItem>()
        try {
            var raw = jsonString.trim()
            if (raw.startsWith("window.google.ac.h(") || (raw.contains("(") && raw.contains(")"))) {
                val start = raw.indexOf('(')
                val end = raw.lastIndexOf(')')
                if (start != -1 && end != -1 && end > start) {
                    raw = raw.substring(start + 1, end).trim()
                }
            }
            val array = JSONArray(raw)
            if (array.length() > 1) {
                val items = array.optJSONArray(1) ?: JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.opt(i)
                    if (item is String) {
                        if (item.isNotBlank()) {
                            suggestions.add(SuggestionItem(query = item))
                        }
                    } else if (item is JSONArray) {
                        val text = item.optString(0)
                        if (text.isNotBlank()) {
                            var chId: String? = null
                            var chTitle: String? = null
                            var chHandle: String? = null
                            var chAvatar: String? = null

                            // Check metadata object in suggestion item array
                            for (k in 1 until item.length()) {
                                val obj = item.optJSONObject(k)
                                if (obj != null) {
                                    val possibleId = obj.optString("zav")
                                    if (possibleId.isNotBlank() && possibleId.startsWith("UC")) {
                                        chId = possibleId
                                    } else {
                                        val zaq = obj.optString("zaq")
                                        val match = ucRegex.find(zaq)
                                        if (match != null) {
                                            chId = match.value
                                        }
                                    }

                                    val title = obj.optString("zao")
                                    if (title.isNotBlank()) {
                                        chTitle = title
                                    }

                                    val handle = obj.optString("zaf")
                                    if (handle.isNotBlank()) {
                                        chHandle = handle
                                    }

                                    val rawAvatar = obj.optString("zai")
                                    if (rawAvatar.isNotBlank()) {
                                        chAvatar = if (rawAvatar.startsWith("//")) "https:$rawAvatar" else rawAvatar
                                    }

                                    if (!chAvatar.isNullOrBlank() || !chId.isNullOrBlank()) {
                                        break
                                    }
                                }
                            }

                            suggestions.add(
                                SuggestionItem(
                                    query = text,
                                    channelId = chId,
                                    channelTitle = chTitle ?: text,
                                    channelHandle = chHandle,
                                    channelAvatarUrl = chAvatar
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return suggestions
    }
}
