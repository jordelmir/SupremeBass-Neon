package com.supreme.android.audio.backend

class NativeDsp {
    companion object {
        init {
            System.loadLibrary("supreme_dsp")
        }
    }

    external fun nativeConfigure(sampleRate: Int)
    external fun nativeSetFrequency(frequency: Double)
    external fun nativeSetWaveform(waveform: Int)
    external fun nativeSetAmplitude(amplitude: Float)
    external fun nativeReset()
    external fun nativeProcessSample(): Float
    external fun nativeProcessBuffer(buffer: FloatArray)
    external fun nativeGetPeak(): Float
    external fun nativeGetRms(): Float
}
