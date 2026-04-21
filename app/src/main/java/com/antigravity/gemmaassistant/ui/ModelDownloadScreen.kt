package com.antigravity.gemmaassistant.ui

import android.app.DownloadManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.gemmaassistant.download.DownloadProgress
import com.antigravity.gemmaassistant.download.GemmaModel

private val BG      = Color(0xFF0D1117)
private val SURFACE = Color(0xFF161B22)
private val ACCENT  = Color(0xFF58A6FF)
private val GREEN   = Color(0xFF56D364)
private val TEXT    = Color(0xFFE6EDF3)
private val HINT    = Color(0xFF8B949E)
private val WARN    = Color(0xFFFFA500)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    hfToken: String,
    onHfTokenChange: (String) -> Unit,
    downloadProgress: Map<GemmaModel, DownloadProgress>,
    downloadedModels: Set<GemmaModel>,
    activeModel: GemmaModel?,
    onDownload: (GemmaModel) -> Unit,
    onCancelDownload: (GemmaModel) -> Unit,
    onUseModel: (GemmaModel) -> Unit,
    onBack: () -> Unit
) {
    var tokenVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 상단 바 ──
            TopAppBar(
                title = {
                    Column {
                        Text("AI 모델 다운로드", color = TEXT, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("앱 내에서 직접 다운로드", color = HINT, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로", tint = HINT)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BG)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── HuggingFace 토큰 입력 ──
                HfTokenCard(
                    token = hfToken,
                    tokenVisible = tokenVisible,
                    onTokenChange = onHfTokenChange,
                    onToggleVisible = { tokenVisible = !tokenVisible }
                )

                // ── 모델 선택 카드 ──
                Text(
                    "모델 선택",
                    color = TEXT,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                GemmaModel.entries.forEach { model ->
                    ModelCard(
                        model = model,
                        isDownloaded = model in downloadedModels,
                        isActive = activeModel == model,
                        progress = downloadProgress[model],
                        onDownload = { onDownload(model) },
                        onCancel = { onCancelDownload(model) },
                        onUse = { onUseModel(model) },
                        isTokenReady = hfToken.isNotBlank()
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ───────── HuggingFace 토큰 카드 ─────────

@Composable
private fun HfTokenCard(
    token: String,
    tokenVisible: Boolean,
    onTokenChange: (String) -> Unit,
    onToggleVisible: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SURFACE),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, null, tint = WARN, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("HuggingFace 액세스 토큰", color = WARN, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Text(
                "Gemma 모델은 라이선스 동의 후 다운로드 가능합니다.\n" +
                "① huggingface.co/google/gemma-4-e4b-it 접속\n" +
                "② 약관 동의 → ③ Settings → Access Tokens에서 토큰 복사",
                color = HINT,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text("hf_...", color = HINT, fontSize = 12.sp) },
                placeholder = { Text("hf_xxxxxxxxxxxxx", color = HINT.copy(0.4f), fontSize = 12.sp) },
                visualTransformation = if (tokenVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleVisible) {
                        Icon(
                            if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = HINT, modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ACCENT,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = TEXT,
                    unfocusedTextColor = TEXT,
                    cursorColor = ACCENT
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
    }
}

// ───────── 모델 카드 ─────────

@Composable
private fun ModelCard(
    model: GemmaModel,
    isDownloaded: Boolean,
    isActive: Boolean,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onUse: () -> Unit,
    isTokenReady: Boolean
) {
    val isDownloading = progress?.isRunning == true

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SURFACE),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) GREEN else Color(0xFF30363D)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.displayName, color = TEXT, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (isActive) {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1A3C1A)) {
                                Text("사용 중", color = GREEN, fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(model.description, color = HINT, fontSize = 12.sp)
                }
            }

            // 스펙 정보
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpecBadge(Icons.Default.Storage, model.sizeText)
                SpecBadge(Icons.Default.Memory, model.ramRequired)
                if (isDownloaded) SpecBadge(Icons.Default.CheckCircle, "저장됨", GREEN)
            }

            // 진행률 바
            if (isDownloading && progress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("다운로드 중...", color = ACCENT, fontSize = 12.sp)
                        Text(
                            "${progress.progressPercent}%  " +
                            "${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                            color = HINT, fontSize = 11.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ACCENT,
                        trackColor = Color(0xFF30363D)
                    )
                }
            }

            // 버튼
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isDownloading -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF7B72)),
                            border = BorderStroke(1.dp, Color(0xFFFF7B72))
                        ) {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("취소", fontSize = 13.sp)
                        }
                    }
                    isDownloaded && !isActive -> {
                        Button(
                            onClick = onUse,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GREEN)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = BG, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("이 모델 사용", color = BG, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f),
                            enabled = isTokenReady,
                            border = BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = HINT, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("재다운로드", color = HINT, fontSize = 12.sp)
                        }
                    }
                    isDownloaded && isActive -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1A3C1A)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = GREEN, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("현재 사용 중인 모델", color = GREEN, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            enabled = isTokenReady,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, null, tint = BG, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isTokenReady) "다운로드  (${model.sizeText})"
                                else "토큰을 먼저 입력하세요",
                                color = BG,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color = HINT
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Text(text, color = color, fontSize = 11.sp)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 0         -> "?"
    bytes < 1024      -> "${bytes}B"
    bytes < 1024*1024 -> "${bytes/1024}KB"
    bytes < 1024*1024*1024 -> "${"%.1f".format(bytes/1024f/1024f)}MB"
    else              -> "${"%.2f".format(bytes/1024f/1024f/1024f)}GB"
}
