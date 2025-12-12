package com.example.humanfollower

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import kotlin.math.max
import kotlin.math.min

class ModelRunner(context: Context) {

    private val interpreter: Interpreter
    private val modelInputSize: Int

    private var lastDetections = mutableListOf<Detection>()
    private var nextId = 1

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.6f
        private const val IOU_THRESHOLD_NMS = 0.5f
        private const val IOU_THRESHOLD_TRACKING = 0.5f
        private const val PERSON_CLASS_INDEX = 0
    }

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, "yolov12n_float32.tflite")
        val options = Interpreter.Options()

        // --- HARDWARE ACCELERATION REMOVED ---
        // We are now only using the CPU.
        options.numThreads = 4

        interpreter = Interpreter(modelBuffer, options)

        val inputTensor = interpreter.getInputTensor(0)
        modelInputSize = inputTensor.shape()[1]
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val inputBuffer = ImageUtils.bitmapToByteBuffer(bitmap, modelInputSize)
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = Array(outputShape[0]) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }

        interpreter.run(inputBuffer, outputBuffer)

        val rawDetections = processOutput(outputBuffer[0])
        return assignTrackingIds(rawDetections)
    }

    private fun processOutput(output: Array<FloatArray>): List<Detection> {
        val numDetections = output[0].size
        val transposedOutput = Array(numDetections) { FloatArray(output.size) }
        for (i in output.indices) {
            for (j in output[i].indices) {
                transposedOutput[j][i] = output[i][j]
            }
        }

        val detections = mutableListOf<Detection>()
        for (prediction in transposedOutput) {
            val classScore = prediction[4 + PERSON_CLASS_INDEX]
            if (classScore > CONFIDENCE_THRESHOLD) {
                val cx = prediction[0]
                val cy = prediction[1]
                val w = prediction[2]
                val h = prediction[3]
                val x1 = (cx - w / 2) / modelInputSize
                val y1 = (cy - h / 2) / modelInputSize
                val x2 = (cx + w / 2) / modelInputSize
                val y2 = (cy + h / 2) / modelInputSize

                detections.add(
                    Detection(x1, y1, x2, y2, classScore, "Person")
                )
            }
        }
        return nonMaxSuppression(detections)
    }

    private fun assignTrackingIds(currentDetections: List<Detection>): List<Detection> {
        val trackedDetections = mutableListOf<Detection>()

        for (currentDet in currentDetections) {
            var bestMatch: Detection? = null
            var bestIoU = 0f

            for (lastDet in lastDetections) {
                val iou = calculateIoU(currentDet, lastDet)
                if (iou > bestIoU) {
                    bestIoU = iou
                    bestMatch = lastDet
                }
            }

            if (bestIoU > IOU_THRESHOLD_TRACKING && bestMatch != null) {
                currentDet.id = bestMatch.id
            } else {
                currentDet.id = nextId++
            }
            trackedDetections.add(currentDet)
        }

        lastDetections.clear()
        lastDetections.addAll(trackedDetections)

        if (trackedDetections.isEmpty()) {
            nextId = 1
        }

        return trackedDetections
    }

    private fun nonMaxSuppression(detections: List<Detection>): List<Detection> {
        val sortedDetections = detections.sortedByDescending { it.score }
        val selectedDetections = mutableListOf<Detection>()
        val active = BooleanArray(sortedDetections.size) { true }
        var numActive = active.size

        for (i in sortedDetections.indices) {
            if (active[i]) {
                selectedDetections.add(sortedDetections[i])
                if (numActive == 1) break
                for (j in i + 1 until sortedDetections.size) {
                    if (active[j]) {
                        val iou = calculateIoU(sortedDetections[i], sortedDetections[j])
                        if (iou > IOU_THRESHOLD_NMS) {
                            active[j] = false
                            numActive--
                        }
                    }
                }
            }
        }
        return selectedDetections
    }

    private fun calculateIoU(a: Detection, b: Detection): Float {
        val x1 = max(a.x1, b.x1); val y1 = max(a.y1, b.y1)
        val x2 = min(a.x2, b.x2); val y2 = min(a.y2, b.y2)
        val intersection = (x2 - x1).coerceAtLeast(0f) * (y2 - y1).coerceAtLeast(0f)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        return intersection / (areaA + areaB - intersection + 1e-6f)
    }
}