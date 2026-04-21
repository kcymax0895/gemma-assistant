package com.antigravity.gemmaassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.antigravity.gemmaassistant.ui.ModelDownloadScreen
import com.antigravity.gemmaassistant.ui.TranslatorScreen
import com.antigravity.gemmaassistant.viewmodel.TranslatorViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0D1117),
                    surface    = Color(0xFF161B22),
                    primary    = Color(0xFF58A6FF)
                )
            ) {
                val uiState by viewModel.uiState.collectAsState()
                var currentScreen by remember { mutableStateOf("translator") }

                LaunchedEffect(Unit) {
                    if (!uiState.isModelLoaded && uiState.downloadedModels.isEmpty()) {
                        currentScreen = "download"
                    }
                }

                val micPermission = rememberPermissionState(
                    android.Manifest.permission.RECORD_AUDIO
                )
                LaunchedEffect(Unit) {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                    }
                }

                if (currentScreen == "download") {
                    ModelDownloadScreen(
                        hfToken = uiState.hfToken,
                        onHfTokenChange = { viewModel.setHfToken(it) },
                        downloadProgress = uiState.downloadProgress,
                        downloadedModels = uiState.downloadedModels,
                        activeModel = uiState.activeModel,
                        onDownload = { model -> viewModel.startModelDownload(model) },
                        onCancelDownload = { model -> viewModel.cancelModelDownload(model) },
                        onUseModel = { model ->
                            viewModel.useDownloadedModel(model)
                            currentScreen = "translator"
                        },
                        onBack = { currentScreen = "translator" }
                    )
                } else {
                    if (!micPermission.status.isGranted) {
                        Box(
                            Modifier.fillMaxSize().systemBarsPadding(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("마이크 권한이 필요합니다.", color = Color(0xFFE6EDF3))
                                Button(onClick = { micPermission.launchPermissionRequest() }) {
                                    Text("권한 허용")
                                }
                            }
                        }
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = Color(0xFF161B22),
                                    contentColor = Color(0xFF58A6FF)
                                ) {
                                    NavigationBarItem(
                                        icon = { Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = null) },
                                        label = { Text("통역") },
                                        selected = currentScreen == "translator",
                                        onClick = { currentScreen = "translator" }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(androidx.compose.material.icons.Icons.Default.Done, contentDescription = null) },
                                        label = { Text("비서") },
                                        selected = currentScreen == "assistant",
                                        onClick = { currentScreen = "assistant" }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                if (currentScreen == "translator") {
                                    TranslatorScreen(
                                        uiState = uiState,
                                        onStartListening  = { viewModel.startListening() },
                                        onStopListening   = { viewModel.stopListening() },
                                        onSwapLanguages   = { viewModel.swapLanguages() },
                                        onSourceLangChange = { viewModel.setSourceLang(it) },
                                        onTargetLangChange = { viewModel.setTargetLang(it) },
                                        onNavigateToSetup  = { currentScreen = "download" },
                                        onClearError       = { viewModel.clearError() }
                                    )
                                } else if (currentScreen == "assistant") {
                                    com.antigravity.gemmaassistant.ui.AssistantScreen(
                                        uiState = uiState,
                                        onSummarizeSms = { viewModel.summarizeSms() },
                                        onSummarizeFile = { uri -> viewModel.summarizeFile(uri) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
