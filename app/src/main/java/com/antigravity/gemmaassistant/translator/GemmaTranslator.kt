package com.antigravity.gemmaassistant.translator

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemma-4-E4B-it (또는 호환 모델) 로컬 번역 엔진.
 * MediaPipe LLM Inference API를 래핑합니다.
 *
 * 모델 파일은 사용자가 .task 형식으로 디바이스에 직접 복사해야 합니다.
 * 권장 경로: /sdcard/Download/gemma-4-e4b-it.task
 */
class GemmaTranslator(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var isInitialized = false

    /**
     * 모델을 초기화합니다.
     * @param modelPath 기기 내 모델 파일 절대 경로 (예: /storage/emulated/0/Download/gemma-4-e4b-it.task)
     * @return 초기화 성공 여부
     */
    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setMaxTopK(40)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            isInitialized = false
            Result.failure(e)
        }
    }

    /**
     * 텍스트를 번역합니다.
     * @param text 번역할 원문
     * @param sourceLang 소스 언어 (예: "Korean", "English")
     * @param targetLang 번역 대상 언어
     * @param onPartialResult 스트리밍 부분 결과 콜백 (실시간 표시용)
     */
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        onPartialResult: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val inference = llmInference ?: return@withContext Result.failure(
            IllegalStateException("모델이 초기화되지 않았습니다. 먼저 initialize()를 호출하세요.")
        )

        if (text.isBlank()) return@withContext Result.success("")

        val prompt = buildTranslationPrompt(text, sourceLang, targetLang)

        try {
            if (onPartialResult != null) {
                // 스트리밍 응답: 실시간으로 번역 결과를 조금씩 표시
                val resultBuilder = StringBuilder()
                inference.generateResponseAsync(prompt) { partialResult, done ->
                    if (partialResult != null) {
                        resultBuilder.append(partialResult)
                        onPartialResult(resultBuilder.toString())
                    }
                }
                // generateResponseAsync는 비동기이므로 generateResponse로 전체 결과 반환
                val fullResult = inference.generateResponse(prompt)
                Result.success(cleanTranslationOutput(fullResult))
            } else {
                val result = inference.generateResponse(prompt)
                Result.success(cleanTranslationOutput(result))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 번역 프롬프트를 구성합니다.
     * Gemma instruction-tuned 모델의 형식을 따릅니다.
     */
    private fun buildTranslationPrompt(text: String, source: String, target: String): String {
        return buildString {
            append("<start_of_turn>user\n")
            append("Translate the following $source text to $target. ")
            append("Provide ONLY the translated text without any explanation, notes, or additional content.\n\n")
            append("Text to translate:\n")
            append(text)
            append("\n<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    /**
     * 텍스트 구조 요약 및 일정 추출 전문 프롬프트 구성
     */
    private fun buildSummarizePrompt(text: String): String {
        return buildString {
            append("<start_of_turn>user\n")
            append("다음 수신된 메시지나 문서의 내용을 읽고 중요한 핵심 내용과 일정을 중심으로 3줄 이내로 한글로 요약해줘. 부가 설명은 생략할 것.\n\n")
            append("내용:\n")
            append(text)
            append("\n<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    /**
     * 비서 기반 문서 요약
     */
    suspend fun summarize(
        text: String,
        onPartialResult: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val inference = llmInference ?: return@withContext Result.failure(
            IllegalStateException("모델이 초기화되지 않았습니다.")
        )
        if (text.isBlank()) return@withContext Result.success("")

        val prompt = buildSummarizePrompt(text)
        try {
            if (onPartialResult != null) {
                val resultBuilder = StringBuilder()
                inference.generateResponseAsync(prompt) { partialResult, done ->
                    if (partialResult != null) {
                        resultBuilder.append(partialResult)
                        onPartialResult(resultBuilder.toString())
                    }
                }
                val fullResult = inference.generateResponse(prompt)
                Result.success(cleanTranslationOutput(fullResult))
            } else {
                val result = inference.generateResponse(prompt)
                Result.success(cleanTranslationOutput(result))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 모델 출력에서 불필요한 토큰/마커를 제거합니다.
     */
    private fun cleanTranslationOutput(raw: String): String {
        return raw
            .replace("<end_of_turn>", "")
            .replace("<start_of_turn>", "")
            .trim()
    }

    /**
     * 모델이 로드되어 있는지 확인합니다.
     */
    fun isReady(): Boolean = isInitialized && llmInference != null

    /**
     * 리소스를 해제합니다.
     */
    fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
