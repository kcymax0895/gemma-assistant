# 🎙️ Gemma 비서 앱 (GemmaAssistant)

회의나 대화 중 음성을 실시간으로 인식하고, **로컬 AI(Gemma-4-E4B-it)**로 즉시 번역하는 완전 오프라인 Android 앱입니다.

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| 🎤 실시간 STT | Android 내장 SpeechRecognizer — 부분 결과 스트리밍 |
| 🤖 로컬 AI 번역 | Gemma-4-E4B-it (MediaPipe LLM Inference) — 인터넷 불필요 |
| 🌍 다국어 지원 | 한국어, 영어, 일본어, 중국어, 스페인어, 프랑스어, 독일어 |
| 🔄 언어 교환 | 원문↔번역 언어 한 번에 교환 |
| 🌑 다크 UI | 다크 테마 + 실시간 마이크 파동 애니메이션 |

## 📱 기기 요구사항

| 항목 | E4B (권장) | E2B (대안) |
|---|---|---|
| RAM | **12GB 이상** | 8GB 이상 |
| 추천 기기 | Galaxy S24 Ultra, Pixel 9 Pro | Galaxy S23 이상 |
| Android 버전 | Android 8.0 (API 26) 이상 | 동일 |

## 🛠️ 빌드 방법

### 1. 사전 요구사항
- **Android Studio Hedgehog** 이상
- **JDK 11** 이상

### 2. 프로젝트 열기
```
Android Studio → File → Open → GemmaAssistant 폴더 선택
```

### 3. 빌드 & 설치
```
File → Sync Project with Gradle Files
Run → Run 'app' (또는 Shift+F10)
```

---

## 📦 Gemma 모델 준비 및 설치

### Step 1. 모델 다운로드
[HuggingFace - Gemma 4](https://huggingface.co/google/gemma-4-e4b-it) 또는 Kaggle에서 MediaPipe `.task` 형식 다운로드

> ⚠️ MediaPipe 전용 `.task` 변환 필요:
> ```bash
> # Python (PC에서 실행)
> pip install mediapipe
> python -c "
> import mediapipe as mp
> # Kaggle 또는 HuggingFace에서 변환 스크립트 사용
> "
> ```
> 자세한 변환 방법: [MediaPipe 공식 문서](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)

### Step 2. 기기에 복사 (ADB)
```bash
# USB 연결 후:
adb push gemma-4-e4b-it.task /sdcard/Download/
```

### Step 3. 앱에서 설정
1. 앱 실행 → 우상단 **⚙️ 설정** 버튼
2. 경로 입력: `/storage/emulated/0/Download/gemma-4-e4b-it.task`
3. **모델 로드** 버튼 클릭 → "AI 준비됨" 뱃지 확인

---

## 🎯 사용 방법

1. 앱 실행 → 마이크 권한 허용
2. 상단에서 **원문 언어**와 **번역 언어** 선택
3. 하단 파란 **🎤 버튼** 탭 → 말하기 시작
4. 화면 중앙에 원문 텍스트, 아래에 번역 결과 실시간 표시
5. 빨간 **⏹ 버튼** 탭 → 인식 중지

---

## 🏗️ 아키텍처

```
MainActivity
  ├── ModelSetupScreen   ← 모델 경로 설정
  └── TranslatorScreen   ← 실시간 번역 메인 화면
        ↕
  TranslatorViewModel
    ├── SpeechManager    ← Android SpeechRecognizer (Flow)
    └── GemmaTranslator  ← MediaPipe LlmInference
```

## 📦 주요 의존성

```
com.google.mediapipe:tasks-genai:0.10.22   ← Gemma 로컬 실행
com.google.accompanist:accompanist-permissions  ← 권한 요청
androidx.compose.material3                  ← UI
```

## 🔒 프라이버시

- **완전 오프라인** 동작 — 음성/텍스트 데이터가 외부 서버로 전송되지 않음
- 단, Android 내장 SpeechRecognizer는 Google 서버를 사용할 수 있음
  - 완전 오프라인 STT를 원한다면 [Vosk](https://alphacephei.com/vosk/android)로 교체 가능

---

## 📝 라이선스
Gemma 모델은 [Google Gemma Terms of Use](https://ai.google.dev/gemma/terms)를 따릅니다.
