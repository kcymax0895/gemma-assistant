package com.antigravity.gemmaassistant.ui

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

private val DarkBackground = Color(0xFF0D1117)
private val CardSurface = Color(0xFF161B22)
private val AccentBlue = Color(0xFF58A6FF)
private val AccentGreen = Color(0xFF56D364)
private val AccentPurple = Color(0xFFBC8CFF)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val DividerColor = Color(0xFF30363D)

@Composable
fun TranslatorScreen(
    uiState: TranslatorUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSwapLanguages: () -> Unit,
    onSourceLangChange: (Language) -> Unit,
    onTargetLangChange: (Language) -> Unit,
    onNavigateToSetup: () -> Unit,
    onClearError: () -> Unit,
) {
    // 마이크 파동 애니메이션
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ── 상단 AppBar ──
            TranslatorTopBar(
                isModelLoaded = uiState.isModelLoaded,
                onSettingsClick = onNavigateToSetup
            )

            // ── 언어 선택 바 ──
            LanguageSelectorBar(
                sourceLang = uiState.sourceLang,
                targetLang = uiState.targetLang,
                onSourceChange = onSourceLangChange,
                onTargetChange = onTargetLangChange,
                onSwap = onSwapLanguages
            )

            // ── 텍스트 영역 ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // 원문
                TextCard(
                    label = "원문 (${uiState.sourceLang.displayName})",
                    text = uiState.sourceText,
                    placeholder = if (uiState.isListening) "듣는 중..." else "녹음 버튼을 눌러 시작하세요",
                    labelColor = AccentBlue,
                    isActive = uiState.isListening
                )

                Spacer(Modifier.height(12.dp))

                // 번역 결과
                TextCard(
                    label = "번역 (${uiState.targetLang.displayName})",
                    text = uiState.translatedText,
                    placeholder = if (uiState.isTranslating) "번역 중..." else "번역 결과가 여기에 표시됩니다",
                    labelColor = AccentGreen,
                    isActive = uiState.isTranslating,
                    showLoadingDot = uiState.isTranslating
                )

                Spacer(Modifier.height(100.dp))
            }
        }

        // ── FAB (녹음 버튼) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            // 파동 효과
            if (uiState.isListening) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.2f))
                        .align(Alignment.Center)
                )
            }

            val fabColor by animateColorAsState(
                if (uiState.isListening) Color(0xFFD73A49) else AccentBlue,
                label = "fabColor"
            )

            FloatingActionButton(
                onClick = { if (uiState.isListening) onStopListening() else onStartListening() },
                containerColor = fabColor,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (uiState.isListening) "중지" else "녹음 시작",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // ── 모델 미설치 배너 ──
        if (!uiState.isModelLoaded && !uiState.isModelLoading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF3D1F00),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFA500), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Gemma 모델이 설정되지 않았습니다. STT만 동작합니다.",
                        color = Color(0xFFFFA500), fontSize = 12.sp
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onNavigateToSetup) {
                        Text("설정", color = Color(0xFFFFA500), fontSize = 12.sp)
                    }
                }
            }
        }

        // ── 에러 스낵바 ──
        uiState.errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = onClearError) {
                        Text("닫기", color = AccentBlue)
                    }
                },
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
                Text(
                    "Gemma 비서",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.width(8.dp))
                // 모델 상태 뱃지
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isModelLoaded) Color(0xFF1A3C1A) else Color(0xFF3C1A1A)
                ) {
                    Text(
                        if (isModelLoaded) "AI 준비됨" else "AI 미연결",
                        color = if (isModelLoaded) AccentGreen else Color(0xFFFF7B72),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
    sourceLang: Language,
    targetLang: Language,
    onSourceChange: (Language) -> Unit,
    onTargetChange: (Language) -> Unit,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageDropdown(
            selected = sourceLang,
            onSelect = onSourceChange,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSwap, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(Icons.Default.SwapHoriz, "언어 교환", tint = AccentPurple, modifier = Modifier.size(28.dp))
        }
        LanguageDropdown(
            selected = targetLang,
            onSelect = onTargetChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LanguageDropdown(
    selected: Language,
    onSelect: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(DividerColor, DividerColor))
            )
        ) {
            Text(selected.displayName, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardSurface)
        ) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName, color = TextPrimary) },
                    onClick = { onSelect(lang); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun TextCard(
    label: String,
    text: String,
    placeholder: String,
    labelColor: Color,
    isActive: Boolean,
    showLoadingDot: Boolean = false
) {
    val borderAlpha by animateFloatAsState(if (isActive) 1f else 0.3f, label = "border")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = labelColor.copy(alpha = borderAlpha)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).heightIn(min = 120.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = labelColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (showLoadingDot) {
                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = labelColor
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (text.isBlank()) {
                Text(
                    placeholder,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Start
                )
            } else {
                Text(
                    text,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                )
            }
        }
    }
}
