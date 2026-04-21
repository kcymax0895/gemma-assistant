package com.antigravity.gemmaassistant.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.gemmaassistant.viewmodel.TranslatorUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

private val DarkBackground = Color(0xFF0D1117)
private val CardSurface = Color(0xFF161B22)
private val AccentPurple = Color(0xFFBC8CFF)
private val AccentGreen = Color(0xFF56D364)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AssistantScreen(
    uiState: TranslatorUiState,
    onSummarizeSms: () -> Unit,
    onSummarizeFile: (android.net.Uri) -> Unit
) {
    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)
    
    // 파일 탐색 런처
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { onSummarizeFile(it) }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "AI 일정 및 메시지 비서",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "스마트폰의 최근 문자 메시지나 텍스트 일정 문서를 Gemma 로컬 AI가 정리하고 요약합니다.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(30.dp))

        // SMS 버튼
        Button(
            onClick = {
                if (smsPermission.status.isGranted) {
                    onSummarizeSms()
                } else {
                    smsPermission.launchPermissionRequest()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Icon(Icons.Default.Message, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("최근 SMS 읽고 요약하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // 파일 버튼
        OutlinedButton(
            onClick = {
                filePickerLauncher.launch(arrayOf("text/plain"))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen)
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("일정/텍스트 파일 선택하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        // 결과 표시 카테고리
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 80.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("요약 결과", color = AccentPurple, fontWeight = FontWeight.Bold)
                    if (uiState.isSummarizing) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPurple, strokeWidth = 2.dp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (uiState.assistantSummarizedText.isBlank()) {
                        Text("버튼을 눌러 분석을 시작하세요...", color = TextSecondary)
                    } else {
                        Text(
                            text = uiState.assistantSummarizedText,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}
