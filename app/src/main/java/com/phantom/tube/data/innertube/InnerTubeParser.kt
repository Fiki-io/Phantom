package com.phantom.tube.data.innertube

import com.phantom.tube.data.innertube.parser.ChannelParser
import com.phantom.tube.data.innertube.parser.CommentsParser
import com.phantom.tube.data.innertube.parser.FeedParser
import com.phantom.tube.data.innertube.parser.SuggestionParser
import com.phantom.tube.data.innertube.parser.WatchNextParser
import com.phantom.tube.data.model.ChannelProfile
import com.phantom.tube.data.model.CommentsResult
import com.phantom.tube.data.model.FeedResult
import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.VideoItem
import org.json.JSONObject

/**
 * Facade entry point for YouTube InnerTube response parsing.
 * Delegates specialized parsing logic to modular domain parsers in [com.phantom.tube.data.innertube.parser].
 */
object InnerTubeParser {

    fun parseSearchResults(jsonString: String): List<VideoItem> =
        FeedParser.parseSearchResults(jsonString)

    fun parseFeedWithContinuation(jsonString: String): FeedResult =
        FeedParser.parseFeedWithContinuation(jsonString)

    fun parseVideoItem(json: JSONObject): VideoItem? =
        FeedParser.parseVideoItem(json)

    fun parseWatchNext(jsonString: String, currentVideoId: String): NextQueue? =
        WatchNextParser.parseWatchNext(jsonString, currentVideoId)

    fun parseWatchNextContinuation(jsonString: String, currentVideoId: String = ""): FeedResult =
        WatchNextParser.parseWatchNextContinuation(jsonString, currentVideoId)

    fun parseComments(jsonString: String): CommentsResult =
        CommentsParser.parseComments(jsonString)

    fun parseSuggestions(jsonString: String): List<String> =
        SuggestionParser.parseSuggestions(jsonString)

    fun parseChannelPage(jsonString: String, fallbackChannelId: String): ChannelProfile? =
        ChannelParser.parseChannelPage(jsonString, fallbackChannelId)

    fun parseChannelContinuation(jsonString: String): FeedResult =
        ChannelParser.parseChannelContinuation(jsonString)
}
