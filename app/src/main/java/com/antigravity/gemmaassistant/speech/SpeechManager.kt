package com.antigravity.gemmaassistant.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android 내장 SpeechRecognizer 래퍼.
 * 부분 결과(partialResults)를 Flow로 방출하여 실시간 STT를 구현합니다.
 */
class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * 음성 인식 스트림을 반환합니다.
     * Flow를 collect하면 인식이 시작되고, collect가 취소되면 인식이 중지됩니다.
     *
     * @param language 인식 언어 코드 (예: "ko-KR", "en-US", "ja-JP", "zh-CN")
     */
    fun recognitionFlow(language: String = "ko-KR"): Flow<SpeechEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechEvent.Error("이 기기는 음성 인식을 지원하지 않습니다."))
            close()
            return@callbackFlow
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Ready)
            }

            override fun onBeginningOfSpeech() {
                trySend(SpeechEvent.Speaking)
            }

            override fun onRmsChanged(rmsdB: Float) {
                trySend(SpeechEvent.RmsChanged(rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                trySend(SpeechEvent.EndOfSpeech)
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 오류"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한 없음"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_NO_MATCH -> "인식 결과 없음"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기 사용 중"
                    SpeechRecognizer.ERROR_SERVER -> "서버 오류"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 타임아웃"
                    else -> "알 수 없는 오류 ($error)"
                }
                trySend(SpeechEvent.Error(msg))
                // NO_MATCH나 SPEECH_TIMEOUT은 다시 시작할 수 있도록 이벤트만 발생
                // 치명적 오류가 아니면 Flow를 닫지 않음
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == SpeechRecognizer.ERROR_CLIENT
                ) {
                    close()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    trySend(SpeechEvent.FinalResult(text))
                }
                // 결과 수신 후 자동으로 재시작
                startListening(language)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    trySend(SpeechEvent.PartialResult(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)
        startListening(language)

        awaitClose {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    private fun startListening(language: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)       // 부분 결과 활성화
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }
        speechRecognizer?.startListening(intent)
    }
}

/**
 * SpeechRecognizer로부터 발생하는 이벤트 타입
 */
sealed class SpeechEvent {
    object Ready : SpeechEvent()
    object Speaking : SpeechEvent()
    object EndOfSpeech : SpeechEvent()
    data class RmsChanged(val rms: Float) : SpeechEvent()
    data class PartialResult(val text: String) : SpeechEvent()
    data class FinalResult(val text: String) : SpeechEvent()
    data class Error(val message: String) : SpeechEvent()
}
