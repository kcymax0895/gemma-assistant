package com.antigravity.webtoonhub.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

class WebtoonScraper {

    suspend fun getNaverWebtoonList(): List<WebtoonItem> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<WebtoonItem>()
        try {
            // 네이버 웹툰 요일별 전체 목록을 제공하는 공식 오픈 API (최신 업데이트 순)
            val apiUrl = "https://comic.naver.com/api/webtoon/titlelist/weekday?order=update"
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val response = conn.inputStream.bufferedReader().readText()
            val root = JSONObject(response)
            val titleListMap = root.optJSONObject("titleListMap") ?: return@withContext emptyList()
            
            // 월요일부터 일요일까지 웹툰만 모음
            val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            for (day in days) {
                val dayArray = titleListMap.optJSONArray(day) ?: continue
                for (i in 0 until dayArray.length()) {
                    val item = dayArray.getJSONObject(i)
                    val titleId = item.optString("titleId")
                    val titleName = item.optString("titleName")
                    val author = item.optString("author")
                    val thumbnail = item.optString("thumbnailUrl")
                    val up = item.optBoolean("up", false)
                    val webUrl = "https://m.comic.naver.com/webtoon/list?titleId=$titleId"

                    // 중복 아이디 제거 안함 (요일별 겹칠 수 있으므로 Compose에서 distinct 처리 예정)
                    resultList.add(
                        WebtoonItem(
                            titleId = titleId,
                            title = titleName,
                            author = author,
                            thumbnailUrl = thumbnail,
                            linkUrl = webUrl,
                            platform = "NAVER",
                            isUpdate = up
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // 중복 제거
        return@withContext resultList.distinctBy { it.titleId }
    }

    suspend fun getKakaoWebtoonList(): List<WebtoonItem> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<WebtoonItem>()
        try {
            // 카카오웹툰의 경우 모바일웹 구조가 보안API로 완전 차단되어 있으므로
            // 기본 연동을 위해 임시로 주요 주소를 WebView 전용 브릿지로 열거나,
            // 공개 페이지 기반 Jsoup으로 타이틀 정보만 임시 스크래핑합니다.
            // 여기서는 MVP 형태로 KakaoWebtoon 초기 URL만 직접 지정해 둡니다.
            resultList.add(
                WebtoonItem(
                    titleId = "kakao_main",
                    title = "카카오웹툰 전체보기",
                    author = "Kakao Entertainment",
                    thumbnailUrl = "https://t1.daumcdn.net/cfile/tistory/241F824757B0A8E32D", // 구글 검색 임시 섬네일용 카카오웹툰 아이콘
                    linkUrl = "https://webtoon.kakao.com/",
                    platform = "KAKAO",
                    isUpdate = true
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext resultList
    }
}
