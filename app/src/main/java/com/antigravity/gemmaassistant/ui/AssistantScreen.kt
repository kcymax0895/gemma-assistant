package com.antigravity.gemmaassistant.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.gemmaassistant.viewmodel.Language
import com.antigravity.gemmaassistant.viewmodel.SUPPORTED_LANGUAGES
import com.antigravity.gemmaassistant.viewmodel.TranslatorUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

private val DarkBackground = Color(0xFF0D1117)
private val CardSurface = Color(0xFF161B22)
private val AccentBlue = Color(0xFF58A6FF)
private val AccentGreen = Color(0xFF56D364)
private val AccentPurple = Color(0xFFBC8CFF)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val DividerColor = Color(0xFF30363D)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AssistantScreen(
    uiState: TranslatorUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSwapLanguages: () -> Unit,
    onSourceLangChange: (Language) -> Unit,
    onTargetLangChange: (Language) -> Unit,
    onNavigateToSetup: () -> Unit,
    onClearError: () -> Unit,
    onSummarizeSms: () -> Unit,
    onSummarizeFile: (android.net.Uri) -> Unit
) {
    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { onSummarizeFile(it) } }
    )

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            
            // Top Bar
            TranslatorTopBar(isModelLoaded = uiState.isModelLoaded, onSettingsClick = onNavigateToSetup)

            // Language Selector
            LanguageSelectorBar(
                sourceLang = uiState.sourceLang,
                targetLang = uiState.targetLang,
                onSourceChange = onSourceLangChange,
                onTargetChange = onTargetLangChange,
                onSwap = onSwapLanguages
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // 원문 카드
                TextCard(
                    label = "입력 (${uiState.sourceLang.displayName} / 분석 문서)",
                    text = uiState.sourceText,
                    placeholder = if (uiState.isListening) "음성 듣는 중..." else "마이크를 누르거나 요약할 문서를 불러오세요.",
                    labelColor = AccentBlue,
                    isActive = uiState.isListening
                )

                Spacer(Modifier.height(12.dp))

                // 결과 카드 (STT 번역 & SMS/파일 요약 동시 지원)
                val isSummarizingNow = uiState.isSummarizing || uiState.assistantSummarizedText.isNotEmpty()
                val activeOutput = if (isSummarizingNow) uiState.assistantSummarizedText else uiState.translatedText
                val outputLabel = if (isSummarizingNow) "AI 분석/요약 결과" else "번역 결과 (${uiState.targetLang.displayName})"
                val isActiveNow = uiState.isTranslating || uiState.isSummarizing

                TextCard(
                    label = outputLabel,
                    text = activeOutput,
                    placeholder = if (isActiveNow) "Gemma 로컬 AI가 생성 중입니다..." else "AI 결과가 여기에 표시됩니다.",
                    labelColor = if (isSummarizingNow) AccentPurple else AccentGreen,
                    isActive = isActiveNow,
                    showLoadingDot = isActiveNow
                )

                Spacer(Modifier.height(30.dp))

                // 액션 버튼류 (SMS/File) - 가로 일렬 배치
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (smsPermission.status.isGranted) onSummarizeSms() else smsPermission.launchPermissionRequest()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Icon(Icons.Default.Message, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SMS 요약", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("text/plain")) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen)
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("텍스트 파일 요약", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(130.dp))
            }
        }

        // FAB 녹음 버튼
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            if (uiState.isListening) {
                Box(
                    modifier = Modifier.size(80.dp).scale(pulseScale).clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.2f)).align(Alignment.Center)
                )
            }
            val fabColor by animateColorAsState(if (uiState.isListening) Color(0xFFD73A49) else AccentBlue, label = "fabColor")
            FloatingActionButton(
                onClick = { if (uiState.isListening) onStopListening() else onStartListening() },
                containerColor = fabColor,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (uiState.isListening) "중지" else "음성 인식 시작",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 모델 미설치 배너
        if (!uiState.isModelLoaded && !uiState.isModelLoading) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(12.dp), color = Color(0xFF3D1F00), tonalElevation = 4.dp
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFA500), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gemma 로컬 LLM이 연결되지 않았습니다. 현재 기본 구글 STT/번역기만 동작합니다.", color = Color(0xFFFFA500), fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onNavigateToSetup) { Text("설정가기", color = Color(0xFFFFA500), fontSize = 12.sp) }
                }
            }
        }

        // 에러 스낵바
        uiState.errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
                action = { TextButton(onClick = onClearError) { Text("닫기", color = AccentBlue) } },
                containerColor = Color(0xFF21262D)
            ) {
                Text(msg, color = TextPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslatorTopBar(isModelLoaded: Boolean, onSettingsClick: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gemma 만능 비서", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isModelLoaded) Color(0xFF1A3C1A) else Color(0xFF3C1A1A)
                ) {
                    Text(
                        if (isModelLoaded) "로컬 AI 준비됨" else "AI 미연결",
                        color = if (isModelLoaded) AccentGreen else Color(0xFFFF7B72),
                        fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, "설정", tint = TextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
    )
}

@Composable
private fun LanguageSelectorBar(
    sourceLang: Language, targetLang: Language,
    onSourceChange: (Language) -> Unit, onTargetChange: (Language) -> Unit, onSwap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageDropdown(selected = sourceLang, onSelect = onSourceChange, modifier = Modifier.weight(1f))
        IconButton(onClick = onSwap, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(Icons.Default.SwapHoriz, "언어 교환", tint = AccentPurple, modifier = Modifier.size(28.dp))
        }
        LanguageDropdown(selected = targetLang, onSelect = onTargetChange, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LanguageDropdown(selected: Language, onSelect: (Language) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(DividerColor, DividerColor)))
        ) {
            Text(selected.displayName, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardSurface)) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(text = { Text(lang.displayName, color = TextPrimary) }, onClick = { onSelect(lang); expanded = false })
            }
        }
    }
}

@Composable
private fun TextCard(
    label: String, text: String, placeholder: String,
    labelColor: Color, isActive: Boolean, showLoadingDot: Boolean = false
) {
    val borderAlpha by animateFloatAsState(if (isActive) 1f else 0.3f, label = "border")
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(width = if (isActive) 1.5.dp else 1.dp, color = labelColor.copy(alpha = borderAlpha))
    ) {
        Column(modifier = Modifier.padding(16.dp).heightIn(min = 120.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (showLoadingDot) {
                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = labelColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (text.isBlank()) {
                Text(placeholder, color = TextSecondary, fontSize = 14.sp)
            } else {
                Text(text, color = TextPrimary, fontSize = 16.sp, lineHeight = 24.sp)
            }
        }
    }
}
