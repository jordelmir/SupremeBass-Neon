#include "native_dsp.h"
#include <jni.h>
#include <android/log.h>

#define TAG "NativeDSP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static dsp::Oscillator g_oscillator;
static dsp::Limiter g_limiter;
static std::atomic<bool> g_running{false};
static int g_sampleRate = 48000;

extern "C" {

JNIEXPORT void JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeConfigure(
    JNIEnv* env, jobject thiz, jint sampleRate) {
    g_sampleRate = sampleRate;
    g_oscillator.configure(sampleRate);
    g_limiter.configure(-0.1f);
    LOGI("Configured: sampleRate=%d", sampleRate);
}

JNIEXPORT void JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeSetFrequency(
    JNIEnv* env, jobject thiz, jdouble frequency) {
    g_oscillator.setFrequency(frequency);
}

JNIEXPORT void JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeSetWaveform(
    JNIEnv* env, jobject thiz, jint waveform) {
    g_oscillator.setWaveform(static_cast<dsp::Waveform>(waveform));
}

JNIEXPORT void JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeSetAmplitude(
    JNIEnv* env, jobject thiz, jfloat amplitude) {
    g_oscillator.setAmplitude(amplitude);
}

JNIEXPORT void JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeReset(
    JNIEnv* env, jobject thiz) {
    g_oscillator.reset();
    g_limiter.reset();
}

JNIEXPORT jfloat JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeProcessSample(
    JNIEnv* env, jobject thiz) {
    float sample = g_oscillator.nextSample();
    sample = g_limiter.process(sample);
    g_oscillator.updateMetrics(sample);
    return sample;
}

JNIEXPORT void JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeProcessBuffer(
    JNIEnv* env, jobject thiz, jfloatArray buffer) {
    if (!buffer) return;
    int len = env->GetArrayLength(buffer);
    jfloat* data = env->GetFloatArrayElements(buffer, nullptr);
    for (int i = 0; i < len; i++) {
        float sample = g_oscillator.nextSample();
        sample = g_limiter.process(sample);
        g_oscillator.updateMetrics(sample);
        data[i] = sample;
    }
    env->ReleaseFloatArrayElements(buffer, data, 0);
}

JNIEXPORT jfloat JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeGetPeak(
    JNIEnv* env, jobject thiz) {
    return g_limiter.getCurrentPeak();
}

JNIEXPORT jfloat JNICALL
Java_com_supremecorp_bass_audio_backend_NativeDsp_nativeGetRms(
    JNIEnv* env, jobject thiz) {
    return g_limiter.getCurrentRms();
}

} // extern "C"
