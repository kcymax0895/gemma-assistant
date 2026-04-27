# Add project specific ProGuard rules here, you can control the set of
# applied configuration files using the proguardFiles setting in build.gradle.

# MediaPipe 클래스 보존
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
