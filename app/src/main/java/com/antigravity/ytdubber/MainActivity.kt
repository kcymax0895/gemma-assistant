package com.antigravity.ytdubber

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.ytdubber.subtitle.DubbingEngine
import com.antigravity.ytdubber.subtitle.SubtitleExtractor
import com.antigravity.ytdubber.subtitle.SubtitleItem
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

fun extractVideoIdUtil(url: String): String {
    val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
    val compiled = java.util.regex.Pattern.compile(pattern)
    val matcher = compiled.matcher(url)
    return if (matcher.find()) matcher.group() else ""
}

class MainActivity : ComponentActivity() {

    private lateinit var dubbingEngine: DubbingEngine
    private val extractor = SubtitleExtractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dubbingEngine = DubbingEngine(this)

        var initialVideoId = ""
        // 공유하기로 들어온 유튜브 인텐트 URL 처리
        if (intent?.action == Intent.ACTION_SEND && "text/plain" == intent.type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            initialVideoId = extractVideoIdUtil(sharedText)
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0D1117),
                    surface = Color(0xFF161B22),
                    primary = Color(0xFF58A6FF)
                )
            ) {
                AutoDubberScreen(
                    initialVideoId = initialVideoId,
                    dubbingEngine = dubbingEngine,
                    extractor = extractor
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 백그라운드에 떠있는데 다른 유튜브 영상을 또 공유한 경우
        if (intent.action == Intent.ACTION_SEND && "text/plain" == intent.type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val videoId = extractVideoIdUtil(sharedText)
            if (videoId.isNotBlank()) {
                // UI 상태를 강제로 업데이트 하려면 ViewModel이나 StateFlow를 써야 하지만,
                // Activity 재생성을 유도하기 위해 재시작할 수 있습니다.
                finish()
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dubbingEngine.shutdown()
    }
}

@Composable
fun AutoDubberScreen(
    initialVideoId: String,
    dubbingEngine: DubbingEngine,
    extractor: SubtitleExtractor
) {
    var videoId by remember { mutableStateOf(initialVideoId) }
    var inputUrl by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("대기 중. 유튜브 앱에서 '공유하기'로 영상을 보내거나 아래에 주소를 입력하세요.") }
    var isDubbingActive by remember { mutableStateOf(false) }
    var subtitles by remember { mutableStateOf<List<SubtitleItem>>(emptyList()) }
    var currentSecond by remember { mutableStateOf(0f) }
    var currentSubtitleText by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    var youTubePlayer: YouTubePlayer? by remember { mutableStateOf(null) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastSpokenIndex by remember { mutableStateOf(-1) }

    // 비디오 ID가 변경되면 자막 추출 및 더빙 엔진을 로드합니다
    LaunchedEffect(videoId) {
        if (videoId.isNotBlank()) {
            statusText = "자막을 추출하고 있습니다..."
            isDubbingActive = false
            currentSubtitleText = ""
            val (langCode, extracted) = extractor.getSubtitles(videoId)
            
            if (extracted.isNotEmpty()) {
                subtitles = extracted
                statusText = "자막 추출 성공(${langCode}). 자체 번역 엔진 로딩 중..."
                
                // ML Kit 언어팩 다운로드 및 번역기 초기화
                val isReady = dubbingEngine.prepareTranslation(langCode)
                if (isReady) {
                    statusText = "⚡ 더빙 완료! 자동 한국어 음성 더빙이 지원됩니다."
                    isDubbingActive = true
                    lastSpokenIndex = -1 // 음성 출력 인덱스 리셋
                } else {
                    statusText = "❌ 번역 엔진 초기화 실패"
                }
            } else {
                statusText = "⚠️ 해당 영상에 자동/수동 자막이 존재하지 않아 더빙할 수 없습니다."
            }
        }
    }

    // 영상 시간에 맞춰 더빙 음성을 계속해서 쏴주는 동기화 루프
    LaunchedEffect(currentSecond, isDubbingActive) {
        if (!isDubbingActive || subtitles.isEmpty()) return@LaunchedEffect
        val currentMs = (currentSecond * 1000).toLong()
        
        for (i in subtitles.indices) {
            val sub = subtitles[i]
            // 오차 여유시간 +200ms
            if (currentMs >= sub.startMs && currentMs <= (sub.startMs + sub.durationMs + 200)) {
                if (lastSpokenIndex != i) {
                    lastSpokenIndex = i
                    
                    // 더빙이 나갈 땐 원본 영상 사운드를 10%로 대폭 줄여서 백그라운드 잡음 처리
                    youTubePlayer?.setVolume(10)
                    
                    coroutineScope.launch {
                        currentSubtitleText = "번역 중..."
                        val translated = dubbingEngine.translateText(sub.text)
                        currentSubtitleText = translated
                        dubbingEngine.speak(translated)
                    }
                }
                break
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (videoId.isNotBlank()) {
                // 커스텀 유튜브 재생 플레이어 탑재
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    factory = { ctx ->
                        val view = YouTubePlayerView(ctx)
                        lifecycleOwner.lifecycle.addObserver(view)
                        view.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                            override fun onReady(player: YouTubePlayer) {
                                youTubePlayer = player
                                player.loadVideo(videoId, 0f)
                            }

                            override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                                currentSecond = second
                            }
                        })
                        view
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("재생 대기중...", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(statusText, color = Color(0xFFE6EDF3), modifier = Modifier.padding(16.dp))

            if (currentSubtitleText.isNotBlank()) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
                ) {
                    Text(
                        text = "🔊 $currentSubtitleText",
                        color = Color(0xFF58A6FF),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text("유튜브 주소 직접 입력") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Button(
                onClick = {
                    val extracted = extractVideoIdUtil(inputUrl)
                    if(extracted.isNotBlank()) {
                        videoId = extracted
                        inputUrl = ""
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("이 주소로 자막 추출 및 더빙 시작")
            }
        }
    }
}
