package com.phantom.tube.data.innertube

import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.VideoDetail
import com.phantom.tube.data.model.VideoItem
import org.json.JSONArray
import org.json.JSONObject

object InnerTubeParser {

    fun parseSearchResults(jsonString: String): List<VideoItem> {
        val items = mutableListOf<VideoItem>()
        try {
            val root = JSONObject(jsonString)
            val contents = root.optJSONObject("contents") ?: return emptyList()
            val twoCol = contents.optJSONObject("twoColumnSearchResultsRenderer")
            val primary = twoCol?.optJSONObject("primaryContents")
            val sectionList = primary?.optJSONObject("sectionListRenderer")
            val sections = sectionList?.optJSONArray("contents") ?: JSONArray()

            for (i in 0 until sections.length()) {
                val sec = sections.optJSONObject(i) ?: continue
                val itemSection = sec.optJSONObject("itemSectionRenderer") ?: continue
                val contentsArray = itemSection.optJSONArray("contents") ?: JSONArray()

                for (j in 0 until contentsArray.length()) {
                    val rawItem = contentsArray.optJSONObject(j) ?: continue
                    parseVideoItem(rawItem)?.let { items.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    fun parseWatchNext(jsonString: String, currentVideoId: String): NextQueue? {
        try {
            val root = JSONObject(jsonString)
            val contents = root.optJSONObject("contents") ?: return null
            val watchNext = contents.optJSONObject("twoColumnWatchNextResults") ?: return null

            // 1. Current Video Details
            val results = watchNext.optJSONObject("results")?.optJSONObject("results")
            val primaryContents = results?.optJSONArray("contents") ?: JSONArray()
            var currentTitle = "Video"
            var currentChannel = ""

            for (i in 0 until primaryContents.length()) {
                val p = primaryContents.optJSONObject(i) ?: continue
                val videoPrimaryInfo = p.optJSONObject("videoPrimaryInfoRenderer")
                if (videoPrimaryInfo != null) {
                    currentTitle = parseRunsText(videoPrimaryInfo.optJSONObject("title"))
                }
                val videoSecondaryInfo = p.optJSONObject("videoSecondaryInfoRenderer")
                if (videoSecondaryInfo != null) {
                    val owner = videoSecondaryInfo.optJSONObject("owner")?.optJSONObject("videoOwnerRenderer")
                    if (owner != null) {
                        currentChannel = parseRunsText(owner.optJSONObject("title"))
                    }
                }
            }

            // 2. Full YouTube Mix Playlist (Photo 1: playlistPanelRenderer)
            val playlistObj = watchNext.optJSONObject("playlist")?.optJSONObject("playlist")
                ?: watchNext.optJSONObject("playlist")?.optJSONObject("playlistPanelRenderer")

            val fullPlaylist = mutableListOf<VideoItem>()
            var detectedCurrentIndex = 0

            if (playlistObj != null) {
                val plContents = playlistObj.optJSONArray("contents") ?: JSONArray()
                detectedCurrentIndex = playlistObj.optInt("currentIndex", 0)

                for (i in 0 until plContents.length()) {
                    val raw = plContents.optJSONObject(i) ?: continue
                    val ppvr = raw.optJSONObject("playlistPanelVideoRenderer") ?: continue
                    val videoId = ppvr.optString("videoId")
                    if (videoId.isBlank()) continue

                    val title = parseRunsText(ppvr.optJSONObject("title"))
                    val channel = parseRunsText(ppvr.optJSONObject("longBylineText") ?: ppvr.optJSONObject("shortBylineText"))
                    val thumb = extractThumbnail(ppvr.optJSONObject("thumbnail"), videoId)
                    val duration = ppvr.optJSONObject("lengthText")?.optString("simpleText") ?: ""

                    val item = VideoItem(
                        id = videoId,
                        title = title,
                        channelTitle = channel,
                        thumbnailUrl = thumb,
                        durationText = duration
                    )
                    fullPlaylist.add(item)

                    val isSelected = ppvr.optBoolean("selected", false)
                    if (isSelected || videoId == currentVideoId) {
                        detectedCurrentIndex = fullPlaylist.lastIndex
                        if (currentTitle == "Video" && title.isNotBlank()) {
                            currentTitle = title
                        }
                        if (currentChannel.isBlank() && channel.isNotBlank()) {
                            currentChannel = channel
                        }
                    }
                }
            }

            var playlistTitle = ""
            if (playlistObj != null) {
                playlistTitle = playlistObj.optString("title")
                if (playlistTitle.isBlank()) {
                    playlistTitle = parseRunsText(playlistObj.optJSONObject("titleText"))
                }
                if (playlistTitle.isBlank()) {
                    playlistTitle = "Mix - $currentTitle"
                }
            }

            // 3. Recommended Videos (Photo 2: Rekomendasi di bawah video)
            val recommendationsList = mutableListOf<VideoItem>()
            val secondary = watchNext.optJSONObject("secondaryResults")?.optJSONObject("secondaryResults")
            val secondaryResults = secondary?.optJSONArray("results") ?: JSONArray()

            for (i in 0 until secondaryResults.length()) {
                val item = secondaryResults.optJSONObject(i) ?: continue
                parseVideoItem(item)?.let {
                    if (it.id != currentVideoId) {
                        recommendationsList.add(it)
                    }
                }
            }

            val currentVideo = VideoItem(
                id = currentVideoId,
                title = currentTitle,
                channelTitle = currentChannel,
                thumbnailUrl = "https://i.ytimg.com/vi/$currentVideoId/hqdefault.jpg"
            )

            return NextQueue(
                currentVideo = currentVideo,
                mixPlaylist = fullPlaylist,
                recommendations = recommendationsList,
                playlistTitle = playlistTitle,
                currentIndex = detectedCurrentIndex
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun parseSuggestions(jsonString: String): List<String> {
        val suggestions = mutableListOf<String>()
        try {
            // format: window.google.ac.h(["query", [["sug1", 0], ["sug2", 0]]])
            val start = jsonString.indexOf('(')
            val end = jsonString.lastIndexOf(')')
            if (start != -1 && end != -1 && end > start) {
                val json = jsonString.substring(start + 1, end)
                val array = JSONArray(json)
                if (array.length() > 1) {
                    val items = array.optJSONArray(1) ?: JSONArray()
                    for (i in 0 until items.length()) {
                        val row = items.optJSONArray(i) ?: continue
                        val text = row.optString(0)
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

    private fun parseVideoItem(json: JSONObject): VideoItem? {
        // Option 1: Classic videoRenderer
        if (json.has("videoRenderer")) {
            val vr = json.getJSONObject("videoRenderer")
            val videoId = vr.optString("videoId")
            if (videoId.isBlank()) return null
            val title = parseRunsText(vr.optJSONObject("title"))
            val channel = parseRunsText(vr.optJSONObject("ownerText"))
            val duration = vr.optJSONObject("lengthText")?.optString("simpleText") ?: ""
            val views = vr.optJSONObject("shortViewCountText")?.optString("simpleText") ?: ""
            val published = vr.optJSONObject("publishedTimeText")?.optString("simpleText") ?: ""
            val thumb = extractThumbnail(vr.optJSONObject("thumbnail"), videoId)

            return VideoItem(
                id = videoId,
                title = title,
                channelTitle = channel,
                thumbnailUrl = thumb,
                durationText = duration,
                viewCountText = views,
                publishedTimeText = published
            )
        }

        // Option 2: compactVideoRenderer (related videos)
        if (json.has("compactVideoRenderer")) {
            val cvr = json.getJSONObject("compactVideoRenderer")
            val videoId = cvr.optString("videoId")
            if (videoId.isBlank()) return null
            val title = parseRunsText(cvr.optJSONObject("title"))
            val channel = parseRunsText(cvr.optJSONObject("longBylineText") ?: cvr.optJSONObject("shortBylineText"))
            val duration = cvr.optJSONObject("lengthText")?.optString("simpleText") ?: ""
            val views = cvr.optJSONObject("shortViewCountText")?.optString("simpleText") ?: ""
            val published = cvr.optJSONObject("publishedTimeText")?.optString("simpleText") ?: ""
            val thumb = extractThumbnail(cvr.optJSONObject("thumbnail"), videoId)

            return VideoItem(
                id = videoId,
                title = title,
                channelTitle = channel,
                thumbnailUrl = thumb,
                durationText = duration,
                viewCountText = views,
                publishedTimeText = published
            )
        }

        // Option 3: Modern lockupViewModel
        if (json.has("lockupViewModel")) {
            val lvm = json.getJSONObject("lockupViewModel")
            val onTap = lvm.optJSONObject("rendererContext")
                ?.optJSONObject("commandContext")
                ?.optJSONObject("onTap")
                ?.optJSONObject("innertubeCommand")
            val watchEndpoint = onTap?.optJSONObject("watchEndpoint")
            val videoId = watchEndpoint?.optString("videoId") ?: ""
            if (videoId.isBlank()) return null

            val metadata = lvm.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")
            val title = metadata?.optJSONObject("title")?.optString("content") ?: ""

            // channel and view count
            val metaRows = metadata?.optJSONObject("metadata")
                ?.optJSONObject("contentMetadataViewModel")
                ?.optJSONArray("metadataRows") ?: JSONArray()

            var channel = ""
            var views = ""
            var published = ""

            if (metaRows.length() > 0) {
                val row0 = metaRows.optJSONObject(0)?.optJSONArray("metadataParts")
                channel = row0?.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
            }
            if (metaRows.length() > 1) {
                val row1 = metaRows.optJSONObject(1)?.optJSONArray("metadataParts")
                views = row1?.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                published = row1?.optJSONObject(1)?.optJSONObject("text")?.optString("content") ?: ""
            }

            return VideoItem(
                id = videoId,
                title = title,
                channelTitle = channel,
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                durationText = "",
                viewCountText = if (views.isNotBlank()) "$views views" else "",
                publishedTimeText = published
            )
        }

        return null
    }

    private fun parseRunsText(obj: JSONObject?): String {
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

    private fun extractThumbnail(obj: JSONObject?, videoId: String): String {
        if (obj == null) return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val thumbnails = obj.optJSONArray("thumbnails")
        if (thumbnails != null && thumbnails.length() > 0) {
            val last = thumbnails.optJSONObject(thumbnails.length() - 1)
            val url = last?.optString("url") ?: ""
            if (url.isNotBlank()) {
                return if (url.startsWith("//")) "https:$url" else url
            }
        }
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }
}
