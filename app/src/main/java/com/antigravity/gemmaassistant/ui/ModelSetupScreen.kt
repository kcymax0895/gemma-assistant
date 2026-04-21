package com.antigravity.gemmaassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF0D1117)
private val SURFACE = Color(0xFF161B22)
private val ACCENT = Color(0xFF58A6FF)
private val TEXT = Color(0xFFE6EDF3)
private val HINT = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSetupScreen(
    currentPath: String,
    isLoading: Boolean,
    error: String?,
    onLoadModel: (String) -> Unit,
    onBack: () -> Unit
) {
    var pathInput by remember { mutableStateOf(currentPath) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 바
            TopAppBar(
                title = { Text("Gemma 모델 설정", color = TEXT, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로", tint = HINT)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BG)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 안내 카드
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SURFACE)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = ACCENT, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("모델 파일 준비 방법", color = ACCENT, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Text(
                            "1. HuggingFace에서 Gemma-4-E4B-it 모델 다운로드\n" +
                            "   (MediaPipe .task 형식 필요)\n\n" +
                            "2. ADB로 기기에 복사:\n" +
                            "   adb push gemma-4-e4b-it.task \\\n" +
                            "   /sdcard/Download/\n\n" +
                            "3. 아래에 기기 내 경로 입력 후 로드\n" +
                            "   예: /storage/emulated/0/Download/gemma-4-e4b-it.task",
                            color = HINT,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // 경로 입력
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = { pathInput = it },
                    label = { Text("모델 파일 경로", color = HINT) },
                    placeholder = { Text("/storage/emulated/0/Download/gemma-4-e4b-it.task", color = HINT.copy(0.5f), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ACCENT,
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = TEXT,
                        unfocusedTextColor = TEXT,
                        cursorColor = ACCENT
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = false,
                    maxLines = 3
                )

                // 에러 표시
                error?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF3C1A1A)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFFF7B72), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = Color(0xFFFF7B72), fontSize = 13.sp)
                        }
                    }
                }

                // 로드 버튼
                Button(
                    onClick = { onLoadModel(pathInput.trim()) },
                    enabled = pathInput.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BG,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("모델 로딩 중...", color = BG, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.PlayArrow, null, tint = BG)
                        Spacer(Modifier.width(8.dp))
                        Text("모델 로드", color = BG, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                // RAM 경고
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1F00))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Memory, null, tint = Color(0xFFFFA500), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "E4B 모델은 약 12GB RAM 필요 (Galaxy S24 Ultra 등 권장)\nE2B 모델은 약 8GB RAM으로 동작 가능",
                            color = Color(0xFFFFA500),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
