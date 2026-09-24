package com.phantom.tube.data.innertube.parser

import com.phantom.tube.data.model.FeedResult
import com.phantom.tube.data.model.VideoItem
import org.json.JSONArray
import org.json.JSONObject

object FeedParser {

    fun parseSearchResults(jsonString: String): List<VideoItem> {
        return parseFeedWithContinuation(jsonString).videos
    }

    fun parseFeedWithContinuation(jsonString: String): FeedResult {
        val items = mutableListOf<VideoItem>()
        var continuationToken: String? = null

        try {
            val root = JSONObject(jsonString)

            // 1. Initial page format: contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents
            val contents = root.optJSONObject("contents")
            val twoCol = contents?.optJSONObject("twoColumnSearchResultsRenderer")
            val primary = twoCol?.optJSONObject("primaryContents")
            val sectionList = primary?.optJSONObject("sectionListRenderer")
            val sections = sectionList?.optJSONArray("contents") ?: JSONArray()

            for (i in 0 until sections.length()) {
                val sec = sections.optJSONObject(i) ?: continue

                // Check for continuation token in initial page
                val continuationItem = sec.optJSONObject("continuationItemRenderer")
                if (continuationItem != null) {
                    val token = continuationItem
                        .optJSONObject("continuationEndpoint")
                        ?.optJSONObject("continuationCommand")
                        ?.optString("token")
                    if (!token.isNullOrBlank()) {
                        continuationToken = token
                    }
                }

                // Check videos in itemSectionRenderer
                val itemSection = sec.optJSONObject("itemSectionRenderer")
                if (itemSection != null) {
                    val contentsArray = itemSection.optJSONArray("contents") ?: JSONArray()
                    for (j in 0 until contentsArray.length()) {
                        val rawItem = contentsArray.optJSONObject(j) ?: continue
                        parseVideoItem(rawItem)?.let { items.add(it) }
                    }
                }
            }

            // 2. Continuation page format: onResponseReceivedCommands
            val commands = root.optJSONArray("onResponseReceivedCommands") ?: JSONArray()
            for (i in 0 until commands.length()) {
                val cmd = commands.optJSONObject(i) ?: continue
                val appendAction = cmd.optJSONObject("appendContinuationItemsAction") ?: continue
                val continuationItems = appendAction.optJSONArray("continuationItems") ?: JSONArray()

                for (j in 0 until continuationItems.length()) {
                    val cItem = continuationItems.optJSONObject(j) ?: continue

                    // Check for next continuation token
                    val nextTokenItem = cItem.optJSONObject("continuationItemRenderer")
                    if (nextTokenItem != null) {
                        val token = nextTokenItem
                            .optJSONObject("continuationEndpoint")
                            ?.optJSONObject("continuationCommand")
                            ?.optString("token")
                        if (!token.isNullOrBlank()) {
                            continuationToken = token
                        }
                    }

                    // Check videos in itemSectionRenderer
                    val itemSection = cItem.optJSONObject("itemSectionRenderer")
                    if (itemSection != null) {
                        val subContents = itemSection.optJSONArray("contents") ?: JSONArray()
                        for (k in 0 until subContents.length()) {
                            val subItem = subContents.optJSONObject(k) ?: continue
                            parseVideoItem(subItem)?.let { items.add(it) }
                        }
                    } else {
                        // Direct video item or lockupViewModel
                        parseVideoItem(cItem)?.let { items.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return FeedResult(
            videos = items,
            continuationToken = continuationToken
        )
    }

    fun parseVideoItem(json: JSONObject): VideoItem? {
        // Option 1: Classic videoRenderer
        if (json.has("videoRenderer")) {
            val vr = json.getJSONObject("videoRenderer")
            val videoId = vr.optString("videoId")
            if (videoId.isBlank()) return null
            val title = InnerTubeHelpers.parseRunsText(vr.optJSONObject("title"))
            val channel = InnerTubeHelpers.parseRunsText(vr.optJSONObject("ownerText"))
            val duration = vr.optJSONObject("lengthText")?.optString("simpleText") ?: ""
            val views = vr.optJSONObject("shortViewCountText")?.optString("simpleText") ?: ""
            val published = vr.optJSONObject("publishedTimeText")?.optString("simpleText") ?: ""
            val thumb = InnerTubeHelpers.extractThumbnail(vr.optJSONObject("thumbnail"), videoId)
            val avatar = InnerTubeHelpers.extractAvatar(vr)
            val channelId = vr.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                ?: vr.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                ?: ""

            return VideoItem(
                id = videoId,
                title = title,
                channelTitle = channel,
                channelId = channelId,
                thumbnailUrl = thumb,
                channelAvatarUrl = avatar,
                durationText = duration,
                viewCountText = InnerTubeHelpers.normalizeViewCount(views),
                publishedTimeText = InnerTubeHelpers.normalizePublishedTime(published)
            )
        }

        // Option 2: compactVideoRenderer (related videos)
        if (json.has("compactVideoRenderer")) {
            val cvr = json.getJSONObject("compactVideoRenderer")
            val videoId = cvr.optString("videoId")
            if (videoId.isBlank()) return null
            val title = InnerTubeHelpers.parseRunsText(cvr.optJSONObject("title"))
            val channel = InnerTubeHelpers.parseRunsText(cvr.optJSONObject("longBylineText") ?: cvr.optJSONObject("shortBylineText"))
            val duration = cvr.optJSONObject("lengthText")?.optString("simpleText") ?: ""
            val views = cvr.optJSONObject("shortViewCountText")?.optString("simpleText") ?: ""
            val published = cvr.optJSONObject("publishedTimeText")?.optString("simpleText") ?: ""
            val thumb = InnerTubeHelpers.extractThumbnail(cvr.optJSONObject("thumbnail"), videoId)
            val avatar = InnerTubeHelpers.extractAvatar(cvr)
            val channelId = cvr.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                ?: cvr.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                ?: ""

            return VideoItem(
                id = videoId,
                title = title,
                channelTitle = channel,
                channelId = channelId,
                thumbnailUrl = thumb,
                channelAvatarUrl = avatar,
                durationText = duration,
                viewCountText = InnerTubeHelpers.normalizeViewCount(views),
                publishedTimeText = InnerTubeHelpers.normalizePublishedTime(published)
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

            if (metaRows.length() == 1) {
                val parts = metaRows.optJSONObject(0)?.optJSONArray("metadataParts")
                if (parts != null) {
                    if (parts.length() >= 2) {
                        views = parts.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                        published = parts.optJSONObject(1)?.optJSONObject("text")?.optString("content") ?: ""
                    } else if (parts.length() == 1) {
                        val txt = parts.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                        if (txt.contains("ditonton", ignoreCase = true) || txt.contains("views", ignoreCase = true) || txt.any { it.isDigit() }) {
                            views = txt
                        } else {
                            channel = txt
                        }
                    }
                }
            } else if (metaRows.length() >= 2) {
                val row0 = metaRows.optJSONObject(0)?.optJSONArray("metadataParts")
                channel = row0?.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                val row1 = metaRows.optJSONObject(1)?.optJSONArray("metadataParts")
                if (row1 != null) {
                    if (row1.length() >= 2) {
                        views = row1.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                        published = row1.optJSONObject(1)?.optJSONObject("text")?.optString("content") ?: ""
                    } else if (row1.length() == 1) {
                        views = row1.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                    }
                }
            }

            var thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val thumbSources = lvm.optJSONObject("contentImage")
                ?.optJSONObject("thumbnailViewModel")
                ?.optJSONObject("image")
                ?.optJSONArray("sources")
            if (thumbSources != null && thumbSources.length() > 0) {
                val lastThumb = thumbSources.optJSONObject(thumbSources.length() - 1)?.optString("url") ?: ""
                if (lastThumb.isNotBlank()) {
                    thumb = if (lastThumb.startsWith("//")) "https:$lastThumb" else lastThumb
                }
            }

            // Extract duration from thumbnail overlays
            var duration = ""
            val overlays = lvm.optJSONObject("contentImage")
                ?.optJSONObject("thumbnailViewModel")
                ?.optJSONArray("overlays") ?: JSONArray()
            for (oi in 0 until overlays.length()) {
                val ov = overlays.optJSONObject(oi) ?: continue
                val bottomOv = ov.optJSONObject("thumbnailBottomOverlayViewModel")
                if (bottomOv != null) {
                    val badges = bottomOv.optJSONArray("badges") ?: JSONArray()
                    for (bi in 0 until badges.length()) {
                        val badge = badges.optJSONObject(bi)?.optJSONObject("thumbnailBadgeViewModel")
                        val text = badge?.optString("text") ?: ""
                        if (text.isNotBlank()) {
                            duration = text
                            break
                        }
                    }
                }
                if (duration.isBlank()) {
                    val timeStatus = ov.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                    val text = timeStatus?.optJSONObject("text")?.optString("simpleText") ?: ""
                    if (text.isNotBlank()) {
                        duration = text
                    }
                }
                if (duration.isNotBlank()) break
            }

            // Extract channelId from lvm
            var channelId = ""
            val bIdMatch = Regex("""\"browseId\":\s*\"(UC[a-zA-Z0-9_-]{22})\"""").find(lvm.toString())
            if (bIdMatch != null) {
                channelId = bIdMatch.groupValues[1]
            }

            val avatar = InnerTubeHelpers.extractAvatar(lvm)

            return VideoItem(
                id = videoId,
                title = title,
                channelTitle = channel,
                channelId = channelId,
                thumbnailUrl = thumb,
                channelAvatarUrl = avatar,
                durationText = duration,
                viewCountText = InnerTubeHelpers.normalizeViewCount(views),
                publishedTimeText = InnerTubeHelpers.normalizePublishedTime(published)
            )
        }

        return null
    }
}
