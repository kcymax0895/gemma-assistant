package com.antigravity.ytdubber.subtitle

import android.content.Context
import android.speech.tts.TextToSpeech
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

class DubbingEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var translator: Translator? = null
    var isReady = false
        private set

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            // 음성 속도 등 조정 가능
            tts?.setSpeechRate(1.1f)
        }
    }

    suspend fun prepareTranslation(sourceLang: String, targetLang: String = TranslateLanguage.KOREAN): Boolean {
        return try {
            // ML Kit의 언어 코드 규격(en, ja 등)으로 매핑
            val validSource = TranslateLanguage.fromLanguageTag(sourceLang) ?: TranslateLanguage.ENGLISH
            
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(validSource)
                .setTargetLanguage(targetLang)
                .build()
            
            translator?.close() // 기존 트랜슬레이터 자원 정리
            translator = Translation.getClient(options)

            val conditions = DownloadConditions.Builder()
                .build()
            translator?.downloadModelIfNeeded(conditions)?.await()
            isReady = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun translateText(text: String): String {
        if (text.isBlank() || translator == null) return text
        return try {
            translator?.translate(text)?.await() ?: text
        } catch (e: Exception) {
            text
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dub_segment")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        translator?.close()
    }
}
