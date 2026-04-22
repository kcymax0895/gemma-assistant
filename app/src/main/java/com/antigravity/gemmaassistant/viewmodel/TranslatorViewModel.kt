package com.antigravity.gemmaassistant.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.gemmaassistant.download.DownloadProgress
import com.antigravity.gemmaassistant.download.GemmaModel
import com.antigravity.gemmaassistant.download.ModelDownloadManager
import com.antigravity.gemmaassistant.speech.SpeechEvent
import com.antigravity.gemmaassistant.speech.SpeechManager
import com.antigravity.gemmaassistant.translator.GemmaTranslator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 지원 언어 목록 */
data class Language(val displayName: String, val code: String, val llmName: String)

val SUPPORTED_LANGUAGES = listOf(
    Language("한국어", "ko-KR", "Korean"),
    Language("영어", "en-US", "English"),
    Language("일본어", "ja-JP", "Japanese"),
    Language("중국어", "zh-CN", "Chinese"),
    Language("스페인어", "es-ES", "Spanish"),
    Language("프랑스어", "fr-FR", "French"),
    Language("독일어", "de-DE", "German"),
)

/** 전체 UI 상태 */
data class TranslatorUiState(
    // 모델 상태
    val modelPath: String = "",
    val isModelLoaded: Boolean = false,
    val isModelLoading: Boolean = false,
    val modelError: String? = null,
    val activeModel: GemmaModel? = null,

    // 다운로드 상태
    val hfToken: String = "",
    val downloadProgress: Map<GemmaModel, DownloadProgress> = emptyMap(),
    val downloadedModels: Set<GemmaModel> = emptySet(),

    // 음성 인식 상태
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val micVolume: Float = 0f,

    // 텍스트
    val sourceText: String = "",
    val translatedText: String = "",
    val isTranslating: Boolean = false,

    // 언어 설정
    val sourceLang: Language = SUPPORTED_LANGUAGES[0],
    val targetLang: Language = SUPPORTED_LANGUAGES[1],

    // 오류
    val errorMessage: String? = null,

    // AI 비서 모드
    val assistantSummarizedText: String = "",
    val isSummarizing: Boolean = false,
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("translator_prefs", android.content.Context.MODE_PRIVATE)

    private val speechManager = SpeechManager(application)
    private val gemmaTranslator = GemmaTranslator(application)
    private val downloadManager = ModelDownloadManager(application)
    private val dataExtractor = com.antigravity.gemmaassistant.assistant.DataExtractorHelper(application)

    // 진행 중인 다운로드 ID 추적
    private val activeDownloads = mutableMapOf<GemmaModel, Long>()

    private val _uiState = MutableStateFlow(
        TranslatorUiState(
            modelPath = prefs.getString("model_path", "") ?: "",
            hfToken = prefs.getString("hf_token", "") ?: "",
            downloadedModels = detectDownloadedModels()
        )
    )
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null
    private var lastFinalText = ""

    init {
        // 저장된 모델 경로가 있으면 자동 로드
        val savedPath = prefs.getString("model_path", "") ?: ""
        if (savedPath.isNotBlank()) {
            loadModel(savedPath)
        }
    }

    // ──────────────────────────── 다운로드 관리 ────────────────────────────

    fun setHfToken(token: String) {
        prefs.edit().putString("hf_token", token).apply()
        _uiState.value = _uiState.value.copy(hfToken = token)
    }

    fun startModelDownload(model: GemmaModel) {
        val token = _uiState.value.hfToken
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "HuggingFace 토큰을 먼저 입력하세요.")
            return
        }

        val downloadId = downloadManager.startDownload(model, token)
        activeDownloads[model] = downloadId

        // 진행률 추적 코루틴
        viewModelScope.launch {
            downloadManager.progressFlow(downloadId).collect { progress ->
                val updated = _uiState.value.downloadProgress.toMutableMap()
                updated[model] = progress
                _uiState.value = _uiState.value.copy(downloadProgress = updated)

                if (progress.isSuccess) {
                    // 다운로드 완료 → 모델 자동 로드
                    val modelPath = downloadManager.getModelPath(model)
                    val newDownloaded = _uiState.value.downloadedModels + model
                    _uiState.value = _uiState.value.copy(downloadedModels = newDownloaded)
                    activeDownloads.remove(model)
                    loadModel(modelPath, model)
                } else if (progress.isFailed) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "다운로드 실패. 토큰과 네트워크를 확인하세요."
                    )
                    activeDownloads.remove(model)
                }
            }
        }
    }

    fun cancelModelDownload(model: GemmaModel) {
        activeDownloads[model]?.let { id ->
            downloadManager.cancelDownload(id)
            activeDownloads.remove(model)
        }
        val updated = _uiState.value.downloadProgress.toMutableMap()
        updated.remove(model)
        _uiState.value = _uiState.value.copy(downloadProgress = updated)
    }

    fun useDownloadedModel(model: GemmaModel) {
        val path = downloadManager.getModelPath(model)
        loadModel(path, model)
    }

    // ──────────────────────────── 모델 로드 ────────────────────────────

    fun loadModel(path: String, model: GemmaModel? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isModelLoading = true, modelError = null)
            val result = gemmaTranslator.initialize(path)
            if (result.isSuccess) {
                prefs.edit().putString("model_path", path).apply()
                _uiState.value = _uiState.value.copy(
                    isModelLoaded = true, isModelLoading = false,
                    modelPath = path, modelError = null,
                    activeModel = model
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isModelLoaded = false, isModelLoading = false,
                    modelError = result.exceptionOrNull()?.message ?: "모델 로드 실패"
                )
            }
        }
    }

    // ──────────────────────────── 음성 인식 ────────────────────────────

    fun startListening() {
        if (_uiState.value.isListening) return
        _uiState.value = _uiState.value.copy(
            isListening = true, sourceText = "", translatedText = "", assistantSummarizedText = "", errorMessage = null
        )
        lastFinalText = ""

        listeningJob = viewModelScope.launch {
            speechManager.recognitionFlow(_uiState.value.sourceLang.code)
                .collect { event -> handleSpeechEvent(event) }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        _uiState.value = _uiState.value.copy(isListening = false, isSpeaking = false, micVolume = 0f)
    }

    private fun handleSpeechEvent(event: SpeechEvent) {
        when (event) {
            is SpeechEvent.Ready -> {}
            is SpeechEvent.Speaking -> _uiState.value = _uiState.value.copy(isSpeaking = true)
            is SpeechEvent.EndOfSpeech -> _uiState.value = _uiState.value.copy(isSpeaking = false)
            is SpeechEvent.RmsChanged -> _uiState.value = _uiState.value.copy(micVolume = event.rms)
            is SpeechEvent.PartialResult -> {
                val combined = if (lastFinalText.isBlank()) event.text else "$lastFinalText ${event.text}"
                _uiState.value = _uiState.value.copy(sourceText = combined)
            }
            is SpeechEvent.FinalResult -> {
                val sentence = if (lastFinalText.isBlank()) event.text else "$lastFinalText ${event.text}"
                lastFinalText = sentence
                _uiState.value = _uiState.value.copy(sourceText = sentence)
                translateText(sentence)
            }
            is SpeechEvent.Error -> {
                if (!event.message.contains("인식 결과 없음") && !event.message.contains("음성 타임아웃")) {
                    _uiState.value = _uiState.value.copy(errorMessage = event.message)
                }
            }
        }
    }

    // ──────────────────────────── 번역 ────────────────────────────

    private fun translateText(text: String) {
        if (!gemmaTranslator.isReady() || text.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTranslating = true)
            val result = gemmaTranslator.translate(
                text = text,
                sourceLang = _uiState.value.sourceLang.llmName,
                targetLang = _uiState.value.targetLang.llmName,
                onPartialResult = { partial ->
                    _uiState.value = _uiState.value.copy(translatedText = partial)
                }
            )
            _uiState.value = _uiState.value.copy(
                isTranslating = false,
                translatedText = result.getOrElse { "번역 실패: ${it.message}" }
            )
        }
    }

    // ──────────────────────────── 언어 변경 ────────────────────────────

    fun setSourceLang(lang: Language) {
        _uiState.value = _uiState.value.copy(sourceLang = lang, sourceText = "", translatedText = "")
    }

    fun setTargetLang(lang: Language) {
        _uiState.value = _uiState.value.copy(targetLang = lang, translatedText = "")
    }

    fun swapLanguages() {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            sourceLang = cur.targetLang, targetLang = cur.sourceLang,
            sourceText = "", translatedText = ""
        )
        if (cur.isListening) { stopListening(); startListening() }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    private fun detectDownloadedModels(): Set<GemmaModel> =
        GemmaModel.entries.filter { downloadManager.isModelDownloaded(it) }.toSet()

    // ──────────────────────────── AI 비서 (요약) ────────────────────────────

    fun summarizeSms(senderFilter: String? = null, daysLimit: Int? = null) {
        if (!gemmaTranslator.isReady()) {
            _uiState.value = _uiState.value.copy(errorMessage = "모델이 초기화되지 않았습니다.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSummarizing = true, assistantSummarizedText = "조건에 맞는 최근 SMS 탐색 중...", sourceText = "문자 메시지(SMS) 내역 분석중...", translatedText = "")
            // 최대 10개를 가져와 요약 문맥에 넘깁니다
            val messages = dataExtractor.getRecentSms(limit = 10, senderFilter = senderFilter, daysLimit = daysLimit)
            if (messages.isEmpty()) {
                _uiState.value = _uiState.value.copy(isSummarizing = false, assistantSummarizedText = "가져올 문자 메시지가 없습니다.")
                return@launch
            }
            // 형식 제작
            val sb = java.lang.StringBuilder()
            messages.forEach { msg ->
                sb.append("Sender: ${msg.sender}\nBody: ${msg.body.take(100)}\n---\n")
            }
            
            _uiState.value = _uiState.value.copy(assistantSummarizedText = "문구 추출 완료. Gemma 분석 중...")
            
            val result = gemmaTranslator.summarize(
                text = sb.toString(),
                onPartialResult = { partial ->
                    _uiState.value = _uiState.value.copy(assistantSummarizedText = partial)
                }
            )
            _uiState.value = _uiState.value.copy(
                isSummarizing = false,
                assistantSummarizedText = result.getOrElse { "요약 실패: ${it.message}" }
            )
        }
    }

    fun summarizeFile(uri: android.net.Uri) {
        if (!gemmaTranslator.isReady()) {
            _uiState.value = _uiState.value.copy(errorMessage = "모델이 초기화되지 않았습니다.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSummarizing = true, assistantSummarizedText = "파일 읽는 중...", sourceText = "파일 첨부 및 읽기 중...", translatedText = "")
            val fileContent = dataExtractor.readTextFile(uri)
            if (fileContent.isBlank()) {
                _uiState.value = _uiState.value.copy(isSummarizing = false, assistantSummarizedText = "파일 내용이 비어있거나 읽을 수 없습니다.")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(assistantSummarizedText = "해당 파일 내용 추출 완료. Gemma 분석 중...")
            
            val result = gemmaTranslator.summarize(
                text = fileContent.take(800), // 네이티브 C++ 백엔드 OOM 방지를 위해 800자로 엄격히 제한
                onPartialResult = { partial ->
                    _uiState.value = _uiState.value.copy(assistantSummarizedText = partial)
                }
            )
            _uiState.value = _uiState.value.copy(
                isSummarizing = false,
                assistantSummarizedText = result.getOrElse { "요약 실패: ${it.message}" }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        gemmaTranslator.close()
    }
}
