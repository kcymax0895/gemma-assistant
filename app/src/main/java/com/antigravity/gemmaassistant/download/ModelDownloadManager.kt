package com.antigravity.gemmaassistant.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 다운로드 가능한 모델 정보 */
enum class GemmaModel(
    val displayName: String,
    val fileName: String,
    val sizeText: String,
    val ramRequired: String,
    val description: String,
    val hfRepo: String,
    val hfFile: String
) {
    E4B(
        displayName = "Gemma-4-E4B (고품질 GPU)",
        fileName = "gemma-gpu-int4.bin",
        sizeText = "약 1.5GB",
        ramRequired = "12GB RAM 이상",
        description = "최고 품질 번역 (GPU 가속). Galaxy S24 Ultra, Pixel 9 Pro 이상 권장",
        hfRepo = "autoocrat0413/gemma-2b-it-gpu-int4-mediapipe",
        hfFile = "gemma-2b-it-gpu-int4.bin"
    ),
    E2B(
        displayName = "Gemma-4-E2B (균형 CPU)",
        fileName = "gemma-cpu-int4.bin",
        sizeText = "약 1.5GB",
        ramRequired = "8GB RAM 이상",
        description = "빠른 속도와 좋은 품질의 균형 (CPU 호환). 대부분의 기기에서 동작",
        hfRepo = "autoocrat0413/gemma-2b-it-gpu-int4-mediapipe",
        hfFile = "gemma-2b-it-cpu-int4.bin"
    )
}

data class DownloadProgress(
    val status: Int,          // DownloadManager.STATUS_*
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localPath: String = ""
) {
    val progressPercent: Int get() =
        if (totalBytes > 0) ((bytesDownloaded.toFloat() / totalBytes) * 100).toInt() else 0

    val isRunning get() = status == DownloadManager.STATUS_RUNNING ||
                          status == DownloadManager.STATUS_PENDING
    val isSuccess get() = status == DownloadManager.STATUS_SUCCESSFUL
    val isFailed  get() = status == DownloadManager.STATUS_FAILED
}

/**
 * Android DownloadManager를 이용한 모델 다운로드 관리자.
 * - 백그라운드 다운로드 (앱 종료 후에도 계속)
 * - 알림 표시
 * - 재개 지원
 * - HuggingFace Bearer 토큰 인증
 */
class ModelDownloadManager(private val context: Context) {

    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * 다운로드를 시작합니다.
     * @param model 다운로드할 모델
     * @param hfToken HuggingFace 액세스 토큰 (허가된 모델 접근용)
     * @return DownloadManager 다운로드 ID (진행 추적에 사용)
     */
    fun startDownload(model: GemmaModel, hfToken: String): Long {
        val url = "https://huggingface.co/${model.hfRepo}/resolve/main/${model.hfFile}"

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("${model.displayName} 다운로드")
            setDescription("Gemma 비서 앱 AI 모델 (${model.sizeText})")
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, model.fileName)
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            addRequestHeader("Authorization", "Bearer $hfToken")
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
        }
        return dm.enqueue(request)
    }

    /**
     * 다운로드 진행률을 Flow로 방출합니다. (1초마다 폴링)
     */
    fun progressFlow(downloadId: Long): Flow<DownloadProgress> = flow {
        while (true) {
            val progress = queryProgress(downloadId)
            emit(progress)
            if (progress.isSuccess || progress.isFailed) break
            delay(1000)
        }
    }

    private fun queryProgress(downloadId: Long): DownloadProgress {
        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val statusIdx   = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val dlIdx       = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalIdx    = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val localIdx    = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                return DownloadProgress(
                    status           = cursor.getInt(statusIdx),
                    bytesDownloaded  = cursor.getLong(dlIdx),
                    totalBytes       = cursor.getLong(totalIdx),
                    localPath        = cursor.getString(localIdx) ?: ""
                )
            }
        }
        return DownloadProgress(DownloadManager.STATUS_FAILED, 0, 0)
    }

    /** 다운로드된 모델의 실제 기기 경로를 반환합니다. */
    fun getModelPath(model: GemmaModel): String {
        return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${model.fileName}"
    }

    /** 모델 파일이 이미 기기에 존재하는지 확인합니다. */
    fun isModelDownloaded(model: GemmaModel): Boolean {
        return java.io.File(getModelPath(model)).exists()
    }

    fun cancelDownload(downloadId: Long) {
        dm.remove(downloadId)
    }
}
