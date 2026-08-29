#pragma once

#include <cstdint>
#include <cmath>
#include <atomic>
#include <array>

namespace dsp {

enum class Waveform : uint8_t {
    SINE = 0,
    SQUARE = 1,
    TRIANGLE = 2,
    SAWTOOTH = 3,
    PULSE = 4,
    MULTI_TONE = 5,
    HARMONIC_STACK = 6,
    CUSTOM = 7
};

class Oscillator {
public:
    Oscillator() = default;

    void configure(int sampleRate) {
        sampleRate_ = sampleRate;
        phaseIncrement_ = (2.0 * M_PI * frequency_) / sampleRate_;
    }

    void setFrequency(double freq) {
        frequency_ = freq;
        phaseIncrement_ = (2.0 * M_PI * freq) / sampleRate_;
    }

    void setWaveform(Waveform wf) { waveform_ = wf; }
    void setAmplitude(float amp) { amplitude_ = amp; }
    void setPhase(double phase) { phase_ = phase; }

    float nextSample() {
        float sample = 0.0f;

        switch (waveform_) {
            case Waveform::SINE:
                sample = std::sin(phase_);
                break;
            case Waveform::SQUARE:
                sample = (phase_ < M_PI) ? 1.0f : -1.0f;
                break;
            case Waveform::TRIANGLE:
                sample = 2.0f * std::abs(2.0f * (float)(phase_ / (2.0 * M_PI)) - 1.0f) - 1.0f;
                break;
            case Waveform::SAWTOOTH:
                sample = 2.0f * (float)(phase_ / (2.0 * M_PI)) - 1.0f;
                break;
            case Waveform::PULSE: {
                float duty = 0.5f;
                float normalized = (float)(phase_ / (2.0 * M_PI));
                sample = (normalized < duty) ? 1.0f : -1.0f;
                break;
            }
            default:
                sample = std::sin(phase_);
                break;
        }

        phase_ += phaseIncrement_;
        if (phase_ >= 2.0 * M_PI) {
            phase_ -= 2.0 * M_PI;
        }

        return sample * amplitude_;
    }

    void reset() {
        phase_ = 0.0;
        peak_ = 0.0f;
        rmsSum_ = 0.0;
        rmsCount_ = 0;
    }

    float peak() const { return peak_; }
    float rms() const {
        if (rmsCount_ == 0) return 0.0f;
        return std::sqrt(rmsSum_ / rmsCount_);
    }

    void updateMetrics(float sample) {
        float absSample = std::abs(sample);
        if (absSample > peak_) peak_ = absSample;
        rmsSum_ += sample * sample;
        rmsCount_++;
    }

private:
    int sampleRate_ = 48000;
    double frequency_ = 1000.0;
    double phase_ = 0.0;
    double phaseIncrement_ = 0.0;
    float amplitude_ = 0.5f;
    Waveform waveform_ = Waveform::SINE;

    float peak_ = 0.0f;
    double rmsSum_ = 0.0;
    int64_t rmsCount_ = 0;
};

class Limiter {
public:
    void configure(float thresholdDb = -0.1f) {
        threshold_ = std::pow(10.0f, thresholdDb / 20.0f);
    }

    float process(float sample) {
        float absSample = std::abs(sample);
        if (absSample > threshold_) {
            sample = threshold_ * (sample / absSample);
        }
        currentPeak_ = std::max(currentPeak_, absSample);
        currentRmsSum_ += sample * sample;
        currentRmsCount_++;
        return sample;
    }

    float getCurrentPeak() const { return currentPeak_; }
    float getCurrentRms() const {
        if (currentRmsCount_ == 0) return 0.0f;
        return std::sqrt(currentRmsSum_ / currentRmsCount_);
    }

    void reset() {
        currentPeak_ = 0.0f;
        currentRmsSum_ = 0.0;
        currentRmsCount_ = 0;
    }

private:
    float threshold_ = 0.988f;
    float currentPeak_ = 0.0f;
    double currentRmsSum_ = 0.0;
    int64_t currentRmsCount_ = 0;
};

} // namespace dsp
