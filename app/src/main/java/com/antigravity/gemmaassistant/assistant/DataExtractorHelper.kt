package com.antigravity.gemmaassistant.assistant

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsMessageData(
    val sender: String,
    val body: String,
    val date: Long
)

class DataExtractorHelper(private val context: Context) {

    /** 
     * 최근 N개의 수신된 SMS 메시지를 읽어옵니다. 
     * 긴급 재난 문자 등은 제외할 수 없으나 가장 최신 내역 위주로 가져옵니다.
     */
    suspend fun getRecentSms(limit: Int = 10, senderFilter: String? = null, daysLimit: Int? = null): List<SmsMessageData> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessageData>()
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)

        var selection: String? = null
        var selectionArgs: Array<String>? = null
        
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        
        if (!senderFilter.isNullOrBlank()) {
            conditions.add("${Telephony.Sms.ADDRESS} LIKE ?")
            args.add("%$senderFilter%")
        }
        
        if (daysLimit != null) {
            val timeLimitInMillis = System.currentTimeMillis() - (daysLimit * 24L * 60L * 60L * 1000L)
            conditions.add("${Telephony.Sms.DATE} >= ?")
            args.add(timeLimitInMillis.toString())
        }
        
        if (conditions.isNotEmpty()) {
            selection = conditions.joinToString(" AND ")
            selectionArgs = args.toTypedArray()
        }

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val address = cursor.getString(addrIdx) ?: "Unknown"
                    val body = cursor.getString(bodyIdx) ?: ""
                    val date = cursor.getLong(dateIdx)
                    messages.add(SmsMessageData(address, body, date))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext messages
    }

    /** 파일을 읽어 내용을 부분적으로 반환합니다. 앱 강제 종료를 막기 위해 최대 크기를 제한합니다. */
    suspend fun readTextFile(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            var text = ""
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val buffer = CharArray(1000)
                val readCount = reader.read(buffer)
                if (readCount > 0) {
                    text = String(buffer, 0, readCount)
                }
            }
            return@withContext text
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "파일 읽기 실패: ${e.message}"
        }
    }
}
