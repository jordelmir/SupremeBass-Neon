package com.supremecorp.bass.cv

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean

data class VisualAcousticData(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val edgeDensity: Float = 0f,
    val colorHistogram: FloatArray = FloatArray(3),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Visual analyzer for camera input.
 *
 * Handles YUV_420_888 → RGB conversion correctly.
 * Implements Sobel edge detection for edge density.
 */
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
                    Log.d(TAG, "Frame $frameCount: brightness=${String.format("%.3f", data.brightness)} " +
                            "contrast=${String.format("%.3f", data.contrast)} " +
                            "edges=${String.format("%.3f", data.edgeDensity)}")
                }
                listener?.invoke(data)
            }
        } finally {
            imageProxy.close()
        }
    }

    private fun processImage(image: Image, rotationDegrees: Int): VisualAcousticData {
        val width = image.width
        val height = image.height
        val totalPixels = width * height

        when (image.format) {
            ImageFormat.YUV_420_888 -> {
                return processYUV420(image, width, height, totalPixels)
            }
            ImageFormat.JPEG -> {
                return processJPEG(image, width, height, totalPixels)
            }
            else -> {
                Log.w(TAG, "Unsupported image format: ${image.format}")
                return VisualAcousticData()
            }
        }
    }

    /**
     * Process YUV_420_888 image — the standard CameraX output format.
     *
     * YUV_420_888 has 3 planes:
     *   Plane 0: Y (luminance) — full resolution
     *   Plane 1: U (Cb) — half resolution, interleaved
     *   Plane 2: V (Cr) — half resolution, interleaved
     *
     * We extract Y for brightness/contrast/edge detection,
     * and compute approximate RGB from YUV for color histogram.
     */
    private fun processYUV420(
        image: Image,
        width: Int,
        height: Int,
        totalPixels: Int
    ): VisualAcousticData {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        // Extract Y (luminance) values
        val yValues = FloatArray(totalPixels)
        var totalBrightness = 0f
        var yIndex = 0

        for (row in 0 until height) {
            for (col in 0 until width) {
                val yOffset = row * yRowStride + col * yPixelStride
                val y = (yBuffer.get(yOffset).toInt() and 0xFF) / 255f
                yValues[yIndex] = y
                totalBrightness += y
                yIndex++
            }
        }

        val avgBrightness = totalBrightness / totalPixels

        // Extract U and V for color histogram (subsampled)
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        var rSum = 0f
        var gSum = 0f
        var bSum = 0f
        var colorPixelCount = 0

        // Sample every 4th pixel for color (U/V are half resolution)
        for (row in 0 until height step 2) {
            for (col in 0 until width step 2) {
                val uvRow = row / 2
                val uvCol = col / 2

                val uOffset = uvRow * uRowStride + uvCol * uPixelStride
                val vOffset = uvRow * vRowStride + uvCol * vPixelStride

                val y = yValues[row * width + col].toDouble()
                val u = (uBuffer.get(uOffset).toInt() and 0xFF) / 255.0 - 0.5
                val v = (vBuffer.get(vOffset).toInt() and 0xFF) / 255.0 - 0.5

                // YUV to RGB conversion (BT.601)
                val r = (y + 1.402 * v).coerceIn(0.0, 1.0)
                val g = (y - 0.344136 * u - 0.714136 * v).coerceIn(0.0, 1.0)
                val b = (y + 1.772 * u).coerceIn(0.0, 1.0)

                rSum += r.toFloat()
                gSum += g.toFloat()
                bSum += b.toFloat()
                colorPixelCount++
            }
        }

        val avgR = if (colorPixelCount > 0) rSum / colorPixelCount else 0f
        val avgG = if (colorPixelCount > 0) gSum / colorPixelCount else 0f
        val avgB = if (colorPixelCount > 0) bSum / colorPixelCount else 0f

        // Calculate contrast from Y channel
        val contrast = calculateContrastFromY(yValues, avgBrightness)

        // Calculate edge density using Sobel on Y channel
        val edgeDensity = calculateEdgeDensity(yValues, width, height)

        return VisualAcousticData(
            brightness = avgBrightness,
            contrast = contrast,
            edgeDensity = edgeDensity,
            colorHistogram = floatArrayOf(avgR, avgG, avgB)
        )
    }

    /**
     * Process JPEG image — fallback for non-YUV formats.
     */
    private fun processJPEG(
        image: Image,
        width: Int,
        height: Int,
        totalPixels: Int
    ): VisualAcousticData {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

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

        val contrast = calculateContrastFromBytes(bytes, avgBrightness)

        return VisualAcousticData(
            brightness = avgBrightness,
            contrast = contrast,
            edgeDensity = 0f, // JPEG edge detection not implemented
            colorHistogram = floatArrayOf(avgR, avgG, avgB)
        )
    }

    private fun calculateContrastFromY(yValues: FloatArray, meanBrightness: Float): Float {
        var variance = 0f
        for (y in yValues) {
            variance += (y - meanBrightness) * (y - meanBrightness)
        }
        return kotlin.math.sqrt(variance / yValues.size)
    }

    private fun calculateContrastFromBytes(bytes: ByteArray, meanBrightness: Float): Float {
        var variance = 0f
        var pixelCount = 0
        var i = 0
        while (i < bytes.size - 2) {
            val brightness = ((bytes[i].toInt() and 0xFF) +
                    (bytes[i + 1].toInt() and 0xFF) +
                    (bytes[i + 2].toInt() and 0xFF)) / (3f * 255f)
            variance += (brightness - meanBrightness) * (brightness - meanBrightness)
            pixelCount++
            i += 3
        }
        return if (pixelCount > 0) kotlin.math.sqrt(variance / pixelCount) else 0f
    }

    /**
     * Calculate edge density using Sobel operator on luminance channel.
     *
     * Sobel kernels:
     *   Gx = [[-1, 0, 1], [-2, 0, 2], [-1, 0, 1]]
     *   Gy = [[-1, -2, -1], [0, 0, 0], [1, 2, 1]]
     *
     * Edge magnitude = sqrt(Gx² + Gy²)
     * Edge density = count(magnitude > threshold) / totalPixels
     */
    private fun calculateEdgeDensity(yValues: FloatArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3) return 0f

        val threshold = 0.1f // Edge detection threshold
        var edgeCount = 0
        var totalProcessed = 0

        // Skip border pixels (Sobel needs 3x3 neighborhood)
        for (row in 1 until height - 1) {
            for (col in 1 until width - 1) {
                val idx = row * width + col

                // Sobel Gx
                val gx = -yValues[(row - 1) * width + (col - 1)] +
                        yValues[(row - 1) * width + (col + 1)] +
                        -2f * yValues[row * width + (col - 1)] +
                        2f * yValues[row * width + (col + 1)] +
                        -yValues[(row + 1) * width + (col - 1)] +
                        yValues[(row + 1) * width + (col + 1)]

                // Sobel Gy
                val gy = -yValues[(row - 1) * width + (col - 1)] +
                        -2f * yValues[(row - 1) * width + col] +
                        -yValues[(row - 1) * width + (col + 1)] +
                        yValues[(row + 1) * width + (col - 1)] +
                        2f * yValues[(row + 1) * width + col] +
                        yValues[(row + 1) * width + (col + 1)]

                val magnitude = kotlin.math.sqrt(gx * gx + gy * gy)
                if (magnitude > threshold) {
                    edgeCount++
                }
                totalProcessed++
            }
        }

        return if (totalProcessed > 0) edgeCount.toFloat() / totalProcessed else 0f
    }
}
