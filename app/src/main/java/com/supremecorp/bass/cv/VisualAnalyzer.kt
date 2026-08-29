package com.supremecorp.bass.cv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

data class VisualAcousticData(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val edgeDensity: Float = 0f,
    val colorHistogram: FloatArray = FloatArray(3),
    val timestamp: Long = System.currentTimeMillis()
)

class VisualAnalyzer : ImageAnalysis.Analyzer {
    private companion object {
        const val TAG = "SupremeBass_VisualAnalyzer"
    }

    private val isAnalyzing = AtomicBoolean(false)
    private var listener: ((VisualAcousticData) -> Unit)? = null
    private var frameCount = 0

    fun setListener(listener: (VisualAcousticData) -> Unit) {
        this.listener = listener
    }

    fun start() {
        isAnalyzing.set(true)
        frameCount = 0
        Log.i(TAG, "Visual analysis started")
    }

    fun stop() {
        isAnalyzing.set(false)
        Log.i(TAG, "Visual analysis stopped after $frameCount frames")
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isAnalyzing.get()) {
            imageProxy.close()
            return
        }

        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val data = processImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                frameCount++
                if (frameCount % 30 == 0) {
                    Log.d(TAG, "Frame $frameCount: brightness=${String.format("%.3f", data.brightness)} contrast=${String.format("%.3f", data.contrast)}")
                }
                listener?.invoke(data)
            }
        } finally {
            imageProxy.close()
        }
    }

    private fun processImage(image: Image, rotationDegrees: Int): VisualAcousticData {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width = image.width
        val height = image.height
        val totalPixels = width * height

        var totalBrightness = 0f
        var rSum = 0f
        var gSum = 0f
        var bSum = 0f

        var i = 0
        while (i < bytes.size - 2) {
            val r = (bytes[i].toInt() and 0xFF) / 255f
            val g = (bytes[i + 1].toInt() and 0xFF) / 255f
            val b = (bytes[i + 2].toInt() and 0xFF) / 255f

            totalBrightness += (r + g + b) / 3f
            rSum += r
            gSum += g
            bSum += b

            i += 3
        }

        val avgBrightness = totalBrightness / totalPixels
        val avgR = rSum / totalPixels
        val avgG = gSum / totalPixels
        val avgB = bSum / totalPixels

        val contrast = calculateContrast(bytes, avgBrightness)

        return VisualAcousticData(
            brightness = avgBrightness,
            contrast = contrast,
            edgeDensity = 0f,
            colorHistogram = floatArrayOf(avgR, avgG, avgB)
        )
    }

    private fun calculateContrast(bytes: ByteArray, meanBrightness: Float): Float {
        var variance = 0f
        var i = 0
        var pixelCount = 0
        while (i < bytes.size - 2) {
            val brightness = ((bytes[i].toInt() and 0xFF) +
                    (bytes[i + 1].toInt() and 0xFF) +
                    (bytes[i + 2].toInt() and 0xFF)) / (3f * 255f)
            variance += (brightness - meanBrightness) * (brightness - meanBrightness)
            pixelCount++
            i += 3
        }
        return kotlin.math.sqrt(variance / pixelCount)
    }
}
