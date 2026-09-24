package com.phantom.tube.data.innertube

import com.phantom.tube.data.model.ChannelProfile
import com.phantom.tube.data.model.ChannelSortChip
import com.phantom.tube.data.model.FeedResult
import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.VideoItem
import org.json.JSONArray
import org.json.JSONObject

object InnerTubeParser {

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
            var currentAvatar = ""

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
                        val ownerThumb = extractThumbnail(owner.optJSONObject("thumbnail"), "")
                        if (ownerThumb.isNotBlank()) {
                            currentAvatar = ownerThumb
                        }
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
            var recContinuationToken: String? = null
            val secondary = watchNext.optJSONObject("secondaryResults")?.optJSONObject("secondaryResults")
            val secondaryResults = secondary?.optJSONArray("results") ?: JSONArray()

            for (i in 0 until secondaryResults.length()) {
                val item = secondaryResults.optJSONObject(i) ?: continue

                val continuationItem = item.optJSONObject("continuationItemRenderer")
                if (continuationItem != null) {
                    val token = continuationItem
                        .optJSONObject("continuationEndpoint")
                        ?.optJSONObject("continuationCommand")
                        ?.optString("token")
                    if (!token.isNullOrBlank()) {
                        recContinuationToken = token
                    }
                }

                val itemSection = item.optJSONObject("itemSectionRenderer")
                if (itemSection != null) {
                    val subContents = itemSection.optJSONArray("contents") ?: JSONArray()
                    for (k in 0 until subContents.length()) {
                        val subItem = subContents.optJSONObject(k) ?: continue
                        val subCont = subItem.optJSONObject("continuationItemRenderer")
                        if (subCont != null) {
                            val token = subCont
                                .optJSONObject("continuationEndpoint")
                                ?.optJSONObject("continuationCommand")
                                ?.optString("token")
                            if (!token.isNullOrBlank()) {
                                recContinuationToken = token
                            }
                        }
                        parseVideoItem(subItem)?.let {
                            if (it.id != currentVideoId) {
                                recommendationsList.add(it)
                            }
                        }
                    }
                } else {
                    parseVideoItem(item)?.let {
                        if (it.id != currentVideoId) {
                            recommendationsList.add(it)
                        }
                    }
                }
            }

            val currentVideo = VideoItem(
                id = currentVideoId,
                title = currentTitle,
                channelTitle = currentChannel,
                channelAvatarUrl = currentAvatar,
                thumbnailUrl = "https://i.ytimg.com/vi/$currentVideoId/hqdefault.jpg"
            )

            return NextQueue(
                currentVideo = currentVideo,
                mixPlaylist = fullPlaylist,
                recommendations = recommendationsList,
                recommendationsContinuationToken = recContinuationToken,
                playlistTitle = playlistTitle,
                currentIndex = detectedCurrentIndex
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun parseWatchNextContinuation(jsonString: String, currentVideoId: String = ""): FeedResult {
        val items = mutableListOf<VideoItem>()
        var continuationToken: String? = null

        try {
            val root = JSONObject(jsonString)
            val commands = root.optJSONArray("onResponseReceivedEndpoints")
                ?: root.optJSONArray("onResponseReceivedCommands")
                ?: JSONArray()

            for (i in 0 until commands.length()) {
                val cmd = commands.optJSONObject(i) ?: continue
                val appendAction = cmd.optJSONObject("appendContinuationItemsAction") ?: continue
                val continuationItems = appendAction.optJSONArray("continuationItems") ?: JSONArray()

                for (j in 0 until continuationItems.length()) {
                    val cItem = continuationItems.optJSONObject(j) ?: continue

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

                    val itemSection = cItem.optJSONObject("itemSectionRenderer")
                    if (itemSection != null) {
                        val subContents = itemSection.optJSONArray("contents") ?: JSONArray()
                        for (k in 0 until subContents.length()) {
                            val subItem = subContents.optJSONObject(k) ?: continue
                            parseVideoItem(subItem)?.let {
                                if (it.id != currentVideoId) items.add(it)
                            }
                        }
                    } else {
                        parseVideoItem(cItem)?.let {
                            if (it.id != currentVideoId) items.add(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return FeedResult(videos = items, continuationToken = continuationToken)
    }

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

    fun parseVideoItem(json: JSONObject): VideoItem? {
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
            val avatar = extractAvatar(vr)
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
            val avatar = extractAvatar(cvr)
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

            val avatar = extractAvatar(lvm)

            return VideoItem(
                id = videoId,
                title = title,
                channelTitle = channel,
                channelId = channelId,
                thumbnailUrl = thumb,
                channelAvatarUrl = avatar,
                durationText = duration,
                viewCountText = if (views.isNotBlank()) views else "",
                publishedTimeText = published
            )
        }

        return null
    }

    private fun extractAvatar(json: JSONObject): String {
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

    fun parseChannelPage(jsonString: String, fallbackChannelId: String): ChannelProfile? {
        try {
            val root = JSONObject(jsonString)
            var channelId = fallbackChannelId
            var title = ""
            var handle = ""
            var avatarUrl = ""
            var bannerUrl = ""
            var subscriberCountText = ""
            var videoCountText = ""
            var description = ""
            var externalLinksText = ""
            val isVerified = false

            // 1. Parse Header
            val header = root.optJSONObject("header")
            val pHeader = header?.optJSONObject("pageHeaderRenderer")
            if (pHeader != null) {
                val content = pHeader.optJSONObject("content")?.optJSONObject("pageHeaderViewModel")
                if (content != null) {
                    title = content.optJSONObject("title")?.optJSONObject("dynamicTextViewModel")
                        ?.optJSONObject("text")?.optString("content") ?: ""
                    if (title.isBlank()) {
                        title = pHeader.optString("pageTitle", "")
                    }

                    // Avatar
                    val avatarSources = content.optJSONObject("image")
                        ?.optJSONObject("decoratedAvatarViewModel")
                        ?.optJSONObject("avatar")
                        ?.optJSONObject("avatarViewModel")
                        ?.optJSONObject("image")
                        ?.optJSONArray("sources")
                    if (avatarSources != null && avatarSources.length() > 0) {
                        avatarUrl = avatarSources.optJSONObject(avatarSources.length() - 1)?.optString("url") ?: ""
                    }

                    // Banner
                    val bannerSources = content.optJSONObject("banner")
                        ?.optJSONObject("imageBannerViewModel")
                        ?.optJSONObject("image")
                        ?.optJSONArray("sources")
                    if (bannerSources != null && bannerSources.length() > 0) {
                        bannerUrl = bannerSources.optJSONObject(bannerSources.length() - 1)?.optString("url") ?: ""
                    }

                    // Metadata (handle, subscribers, videos)
                    val metadataRows = content.optJSONObject("metadata")
                        ?.optJSONObject("contentMetadataViewModel")
                        ?.optJSONArray("metadataRows") ?: JSONArray()
                    if (metadataRows.length() > 0) {
                        val row0 = metadataRows.optJSONObject(0)?.optJSONArray("metadataParts")
                        if (row0 != null && row0.length() > 0) {
                            handle = row0.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                        }
                    }
                    if (metadataRows.length() > 1) {
                        val row1 = metadataRows.optJSONObject(1)?.optJSONArray("metadataParts")
                        if (row1 != null) {
                            if (row1.length() > 0) {
                                subscriberCountText = row1.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                            }
                            if (row1.length() > 1) {
                                videoCountText = row1.optJSONObject(1)?.optJSONObject("text")?.optString("content") ?: ""
                            }
                        }
                    }

                    // Description
                    description = content.optJSONObject("description")
                        ?.optJSONObject("descriptionPreviewViewModel")
                        ?.optJSONObject("description")
                        ?.optString("content") ?: ""

                    // External links / attribution
                    externalLinksText = content.optJSONObject("attribution")
                        ?.optJSONObject("attributionViewModel")
                        ?.optJSONObject("text")
                        ?.optString("content") ?: ""
                }
            }

            // Fallback c4TabbedHeaderRenderer
            val c4Header = header?.optJSONObject("c4TabbedHeaderRenderer")
            if (c4Header != null) {
                if (title.isBlank()) title = c4Header.optString("title", "")
                if (avatarUrl.isBlank()) avatarUrl = extractThumbnail(c4Header.optJSONObject("avatar"), "")
                if (bannerUrl.isBlank()) bannerUrl = extractThumbnail(c4Header.optJSONObject("banner") ?: c4Header.optJSONObject("tvBanner"), "")
                if (subscriberCountText.isBlank()) subscriberCountText = c4Header.optJSONObject("subscriberCountText")?.optString("simpleText") ?: ""
                val cId = c4Header.optString("channelId")
                if (cId.isNotBlank()) channelId = cId
            }

            if (title.isBlank() && fallbackChannelId.isNotBlank()) {
                title = fallbackChannelId
            }

            // 2. Parse Tabs (find video tab params and parse active tab contents)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs") ?: JSONArray()

            var videoTabParams: String? = null
            var activeTabContent: JSONObject? = null
            var firstTabContent: JSONObject? = null

            for (i in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
                val tabTitle = tab.optString("title", "")
                val endpoint = tab.optJSONObject("endpoint")?.optJSONObject("browseEndpoint")
                val params = endpoint?.optString("params")

                if (tabTitle.equals("Video", ignoreCase = true) || tabTitle.equals("Videos", ignoreCase = true)) {
                    if (!params.isNullOrBlank()) {
                        videoTabParams = params
                    }
                }

                if (tab.optBoolean("selected", false)) {
                    activeTabContent = tab.optJSONObject("content")
                }
                if (firstTabContent == null) {
                    firstTabContent = tab.optJSONObject("content")
                }
            }

            val targetContent = activeTabContent ?: firstTabContent
            val videos = mutableListOf<VideoItem>()
            val sortChips = mutableListOf<ChannelSortChip>()
            var continuationToken: String? = null

            if (targetContent != null) {
                val richGrid = targetContent.optJSONObject("richGridRenderer")
                if (richGrid != null) {
                    // Extract chips from chipBarViewModel
                    val chipBar = richGrid.optJSONObject("header")?.optJSONObject("chipBarViewModel")
                    val chipsArray = chipBar?.optJSONArray("chips") ?: JSONArray()
                    for (c in 0 until chipsArray.length()) {
                        val chipObj = chipsArray.optJSONObject(c)?.optJSONObject("chipViewModel") ?: continue
                        val cText = chipObj.optString("text")
                        val cSelected = chipObj.optBoolean("selected", false)
                        val cToken = chipObj.optJSONObject("tapCommand")
                            ?.optJSONObject("innertubeCommand")
                            ?.optJSONObject("continuationCommand")
                            ?.optString("token") ?: ""
                        if (cText.isNotBlank()) {
                            sortChips.add(ChannelSortChip(title = cText, continuationToken = cToken, isSelected = cSelected))
                        }
                    }

                    // Extract videos and continuation from contents
                    val contents = richGrid.optJSONArray("contents") ?: JSONArray()
                    for (j in 0 until contents.length()) {
                        val itm = contents.optJSONObject(j) ?: continue
                        val cItem = itm.optJSONObject("continuationItemRenderer")
                        if (cItem != null) {
                            val token = cItem.optJSONObject("continuationEndpoint")
                                ?.optJSONObject("continuationCommand")
                                ?.optString("token")
                            if (!token.isNullOrBlank()) {
                                continuationToken = token
                            }
                        }
                        val richContent = itm.optJSONObject("richItemRenderer")?.optJSONObject("content")
                        if (richContent != null) {
                            parseVideoItem(richContent)?.let { videos.add(it) }
                        }
                    }
                }

                // Fallback: sectionListRenderer
                val sectionList = targetContent.optJSONObject("sectionListRenderer")
                if (sectionList != null) {
                    val sContents = sectionList.optJSONArray("contents") ?: JSONArray()
                    for (s in 0 until sContents.length()) {
                        val sec = sContents.optJSONObject(s) ?: continue
                        val itemSec = sec.optJSONObject("itemSectionRenderer")
                        val isContents = itemSec?.optJSONArray("contents") ?: JSONArray()
                        for (k in 0 until isContents.length()) {
                            val raw = isContents.optJSONObject(k) ?: continue
                            // check continuation
                            val cItem = raw.optJSONObject("continuationItemRenderer")
                            if (cItem != null) {
                                val token = cItem.optJSONObject("continuationEndpoint")
                                    ?.optJSONObject("continuationCommand")
                                    ?.optString("token")
                                if (!token.isNullOrBlank()) continuationToken = token
                            }
                            parseVideoItem(raw)?.let { videos.add(it) }
                        }
                    }
                }
            }

            return ChannelProfile(
                id = channelId,
                title = title,
                handle = handle,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                subscriberCountText = subscriberCountText,
                videoCountText = videoCountText,
                description = description,
                externalLinksText = externalLinksText,
                isVerified = isVerified,
                videos = videos,
                continuationToken = continuationToken,
                videoTabParams = videoTabParams,
                sortChips = sortChips
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun parseChannelContinuation(jsonString: String): FeedResult {
        val items = mutableListOf<VideoItem>()
        var nextContinuationToken: String? = null
        try {
            val root = JSONObject(jsonString)
            val actions = root.optJSONArray("onResponseReceivedActions")
                ?: root.optJSONArray("onResponseReceivedCommands") ?: JSONArray()
            for (i in 0 until actions.length()) {
                val act = actions.optJSONObject(i) ?: continue
                val appendAction = act.optJSONObject("appendContinuationItemsAction")
                    ?: act.optJSONObject("reloadContinuationItemsCommand") ?: continue
                val continuationItems = appendAction.optJSONArray("continuationItems") ?: JSONArray()
                for (j in 0 until continuationItems.length()) {
                    val item = continuationItems.optJSONObject(j) ?: continue
                    val cItem = item.optJSONObject("continuationItemRenderer")
                    if (cItem != null) {
                        val token = cItem.optJSONObject("continuationEndpoint")
                            ?.optJSONObject("continuationCommand")
                            ?.optString("token")
                        if (!token.isNullOrBlank()) {
                            nextContinuationToken = token
                        }
                    }
                    val richContent = item.optJSONObject("richItemRenderer")?.optJSONObject("content")
                    if (richContent != null) {
                        parseVideoItem(richContent)?.let { items.add(it) }
                    } else {
                        parseVideoItem(item)?.let { items.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return FeedResult(videos = items, continuationToken = nextContinuationToken)
    }
}

