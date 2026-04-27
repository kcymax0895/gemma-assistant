package com.antigravity.ytdubber.subtitle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

data class SubtitleItem(
    val startMs: Long,
    val durationMs: Long,
    val text: String
)

class SubtitleExtractor {
    suspend fun getSubtitles(videoId: String): Pair<String, List<SubtitleItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            val scripts = doc.select("script")
            var playerResponseJsonStr = ""
            for (script in scripts) {
                val data = script.data()
                if (data.contains("ytInitialPlayerResponse = ")) {
                    val startIdx = data.indexOf("ytInitialPlayerResponse = ") + 26
                    var endIdx = data.indexOf("};", startIdx) + 1
                    if (endIdx <= 0) {
                        endIdx = data.lastIndexOf("}") + 1
                    }
                    if (endIdx > 0) {
                        playerResponseJsonStr = data.substring(startIdx, endIdx)
                    }
                    break
                }
            }

            if (playerResponseJsonStr.isEmpty()) throw Exception("자막 정보를 찾을 수 없습니다.")

            val root = JSONObject(playerResponseJsonStr)
            val captions = root.optJSONObject("captions")
                ?.optJSONObject("playerCaptionsTracklistRenderer")
                ?.optJSONArray("captionTracks") ?: throw Exception("해당 영상에 접근 가능한 자막이 없습니다.")

            var targetTrackUrl = ""
            var languageCode = "en" // 기본값
            
            for (i in 0 until captions.length()) {
                val track = captions.getJSONObject(i)
                val langCode = track.optString("languageCode")
                if (langCode.startsWith("en") || track.optString("kind") == "asr") {
                    targetTrackUrl = track.optString("baseUrl")
                    languageCode = langCode
                    break
                }
            }
            if (targetTrackUrl.isEmpty() && captions.length() > 0) {
                val track = captions.getJSONObject(0)
                targetTrackUrl = track.optString("baseUrl")
                languageCode = track.optString("languageCode", "en")
            }

            targetTrackUrl += "&fmt=json3"

            val captionUrl = URL(targetTrackUrl)
            val conn = captionUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            val responseReader = conn.inputStream.bufferedReader()
            val responseStr = responseReader.readText()
            responseReader.close()

            val jsonResponse = JSONObject(responseStr)
            val events = jsonResponse.optJSONArray("events") ?: return@withContext Pair(languageCode, emptyList())

            val resultList = mutableListOf<SubtitleItem>()
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                val tStartMs = event.optLong("tStartMs", 0)
                val dDurationMs = event.optLong("dDurationMs", 0)
                
                val segs = event.optJSONArray("segs")
                if (segs != null && segs.length() > 0) {
                    val textBuilder = java.lang.StringBuilder()
                    for (s in 0 until segs.length()) {
                        textBuilder.append(segs.getJSONObject(s).optString("utf8", ""))
                    }
                    val text = textBuilder.toString().trim()
                    if (text.isNotBlank() && text != "\n") {
                        resultList.add(SubtitleItem(tStartMs, dDurationMs, text))
                    }
                }
            }
            return@withContext Pair(languageCode, resultList)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair("en", emptyList())
        }
    }
}
