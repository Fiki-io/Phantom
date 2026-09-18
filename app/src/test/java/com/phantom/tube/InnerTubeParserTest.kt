package com.phantom.tube

import com.phantom.tube.data.innertube.InnerTubeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InnerTubeParserTest {

    @Test
    fun testParseSuggestions() {
        val sampleJson = """window.google.ac.h(["kotlin",[["kotlin android",0],["kotlin tutorial",0],["kotlin coroutines",0]]])"""
        val suggestions = InnerTubeParser.parseSuggestions(sampleJson)

        assertEquals(3, suggestions.size)
        assertEquals("kotlin android", suggestions[0])
        assertEquals("kotlin tutorial", suggestions[1])
        assertEquals("kotlin coroutines", suggestions[2])
    }

    @Test
    fun testParseVideoRendererSearch() {
        val sampleSearchJson = """
        {
          "contents": {
            "twoColumnSearchResultsRenderer": {
              "primaryContents": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "itemSectionRenderer": {
                        "contents": [
                          {
                            "videoRenderer": {
                              "videoId": "test12345",
                              "title": { "runs": [{ "text": "Sample Test Video" }] },
                              "ownerText": { "runs": [{ "text": "Sample Channel" }] },
                              "lengthText": { "simpleText": "10:30" },
                              "shortViewCountText": { "simpleText": "1.2M views" },
                              "publishedTimeText": { "simpleText": "2 days ago" },
                              "thumbnail": {
                                "thumbnails": [
                                  { "url": "https://i.ytimg.com/vi/test12345/hqdefault.jpg" }
                                ]
                              }
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val results = InnerTubeParser.parseSearchResults(sampleSearchJson)
        assertEquals(1, results.size)
        val item = results[0]
        assertEquals("test12345", item.id)
        assertEquals("Sample Test Video", item.title)
        assertEquals("Sample Channel", item.channelTitle)
        assertEquals("10:30", item.durationText)
        assertEquals("1.2M views", item.viewCountText)
    }

    @Test
    fun testParseLockupViewModelQueue() {
        val sampleWatchNextJson = """
        {
          "contents": {
            "twoColumnWatchNextResults": {
              "results": {
                "results": {
                  "contents": [
                    {
                      "videoPrimaryInfoRenderer": {
                        "title": { "runs": [{ "text": "Current Playing Video" }] }
                      }
                    }
                  ]
                }
              },
              "secondaryResults": {
                "secondaryResults": {
                  "results": [
                    {
                      "lockupViewModel": {
                        "rendererContext": {
                          "commandContext": {
                            "onTap": {
                              "innertubeCommand": {
                                "watchEndpoint": {
                                  "videoId": "nextVid999"
                                }
                              }
                            }
                          }
                        },
                        "metadata": {
                          "lockupMetadataViewModel": {
                            "title": { "content": "Next Recommended Video" },
                            "metadata": {
                              "contentMetadataViewModel": {
                                "metadataRows": [
                                  { "metadataParts": [{ "text": { "content": "Next Channel" } }] },
                                  { "metadataParts": [{ "text": { "content": "500K" } }, { "text": { "content": "1 day ago" } }] }
                                ]
                              }
                            }
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val queue = InnerTubeParser.parseWatchNext(sampleWatchNextJson, "current123")
        assertNotNull(queue)
        assertEquals("current123", queue!!.currentVideo.id)
        assertEquals("Current Playing Video", queue.currentVideo.title)
        assertEquals(1, queue.upNext.size)
        val nextItem = queue.upNext[0]
        assertEquals("nextVid999", nextItem.id)
        assertEquals("Next Recommended Video", nextItem.title)
        assertEquals("Next Channel", nextItem.channelTitle)
    }
}
