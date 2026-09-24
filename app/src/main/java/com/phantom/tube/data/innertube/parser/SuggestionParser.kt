package com.phantom.tube.data.innertube.parser

import org.json.JSONArray

object SuggestionParser {

    fun parseSuggestions(jsonString: String): List<String> {
        val suggestions = mutableListOf<String>()
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
                            suggestions.add(item)
                        }
                    } else if (item is JSONArray) {
                        val text = item.optString(0)
                        if (text.isNotBlank()) {
                            suggestions.add(text)
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
