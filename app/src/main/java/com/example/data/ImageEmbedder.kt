package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class ImageEmbedder(private val context: Context) {
    private val tag = "ImageEmbedder"
    private var interpreter: Interpreter? = null

    init {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true) // Enables GPU/NPU acceleration with CPU fallback
            }
            interpreter = Interpreter(loadModelFile(context), options)
            Log.d(tag, "Successfully initialized TensorFlow Lite Interpreter with NNAPI acceleration")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TensorFlow Lite Interpreter", e)
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("mobilenet_v3.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Loads a bitmap from file path or content/file Uri, scales it to 224x224, 
     * executes the MobileNet V3 Small TFLite model, L2-normalizes the output and
     * returns a 1024-float embedding array.
     */
    fun getEmbedding(pathOrUri: String): FloatArray? {
        val interpreterInstance = interpreter ?: return null
        return try {
            val rawBitmap = loadBitmap(pathOrUri) ?: return null
            val resized = Bitmap.createScaledBitmap(rawBitmap, 224, 224, true)
            
            // Allocate direct byte buffer (1 * 224 * 224 * 3 * 4 bytes per float)
            val inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            val intValues = IntArray(224 * 224)
            resized.getPixels(intValues, 0, 224, 0, 0, 224, 224)
            for (pixel in intValues) {
                // Normalize pixels to 0.0f - 1.0f as per MobileNet V3 standards
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }
            
            val outputBuffer = Array(1) { FloatArray(1024) }
            
            // Run inference
            interpreterInstance.run(inputBuffer, outputBuffer)
            val rawVector = outputBuffer[0]
            
            // Compute L2 normalization
            var sumSquares = 0.0f
            for (v in rawVector) {
                sumSquares += v * v
            }
            val norm = sqrt(sumSquares)
            if (norm == 0.0f) {
                rawVector
            } else {
                FloatArray(rawVector.size) { i -> rawVector[i] / norm }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error generating embedding for $pathOrUri", e)
            null
        }
    }

    /**
     * Loads raw bitmap from Uri or relative/absolute path
     */
    fun loadBitmap(pathOrUri: String): Bitmap? {
        return try {
            if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                BitmapFactory.decodeFile(pathOrUri)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error decoding bitmap from $pathOrUri", e)
            null
        }
    }

    /**
     * Frees resources associated with the interpreter
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        /**
         * Calculates the standard Laplacian variance of a bitmap to detect blur level.
         * Resizes the image to 400x400 to maintain blur features while avoiding heavy CPU cycles.
         */
        fun calculateLaplacianVariance(bitmap: Bitmap): Double {
            return try {
                val maxDim = 400
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val (w, h) = if (ratio > 1f) {
                        Pair(maxDim, (maxDim / ratio).toInt())
                    } else {
                        Pair((maxDim * ratio).toInt(), maxDim)
                    }
                    Bitmap.createScaledBitmap(bitmap, w, h, true)
                } else {
                    bitmap
                }

                val width = scaled.width
                val height = scaled.height
                val pixels = IntArray(width * height)
                scaled.getPixels(pixels, 0, width, 0, 0, width, height)

                // Get grayscale values using standard luminance formula
                val gray = DoubleArray(width * height)
                for (i in pixels.indices) {
                    val p = pixels[i]
                    val r = (p shr 16) and 0xff
                    val g = (p shr 8) and 0xff
                    val b = p and 0xff
                    gray[i] = r * 0.299 + g * 0.587 + b * 0.114
                }

                // Convolve with the 3x3 Laplacian kernel:
                // [ 0  1  0 ]
                // [ 1 -4  1 ]
                // [ 0  1  0 ]
                val laplacianResponse = DoubleArray((width - 2) * (height - 2))
                var sum = 0.0
                var k = 0
                for (y in 1 until height - 1) {
                    val yOffset = y * width
                    for (x in 1 until width - 1) {
                        val center = gray[yOffset + x]
                        val valL = (gray[yOffset - width + x] + // Top
                                    gray[yOffset + width + x] + // Bottom
                                    gray[yOffset + x - 1] +     // Left
                                    gray[yOffset + x + 1] -     // Right
                                    4.0 * center)
                        laplacianResponse[k] = valL
                        sum += valL
                        k++
                    }
                }

                if (k == 0) return 0.0

                val mean = sum / k
                var varianceSum = 0.0
                for (i in 0 until k) {
                    val diff = laplacianResponse[i] - mean
                    varianceSum += diff * diff
                }

                varianceSum / k
            } catch (e: Exception) {
                Log.e("ImageEmbedder", "Error computing Laplacian variance", e)
                0.0
            }
        }
    }
}
