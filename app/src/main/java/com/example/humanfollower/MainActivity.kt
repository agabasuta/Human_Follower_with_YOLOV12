package com.example.humanfollower

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlay: OverlayView
    private lateinit var modelRunner: ModelRunner
    private lateinit var tts: TextToSpeech

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var lastSpoken = ""

    // --- STATE MANAGEMENT ---
    private var targetPersonId: Int = -1 // -1 means no target

    private var lastSpokenTime = 0L
    private var fpsTimer = System.currentTimeMillis()
    private var fpsFrameCount = 0
    private var smoothedFps = 0f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlay = findViewById(R.id.overlay)
        modelRunner = ModelRunner(this)
        tts = TextToSpeech(this, this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun speak(text: String) {
        val currentTime = System.currentTimeMillis()
        // --- VOICE DELAY: You can change 1000 to a smaller number like 800 for a faster feel ---
        if (currentTime - lastSpokenTime < 2000) {
            return
        }

        if (text != lastSpoken && text.lowercase() != "no person detected") {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpoken = text
            lastSpokenTime = currentTime
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    // FPS calculation
                    fpsFrameCount++
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - fpsTimer >= 1000) {
                        smoothedFps = fpsFrameCount.toFloat()
                        fpsFrameCount = 0
                        fpsTimer = currentTime
                    }

                    val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
                    val detections = modelRunner.detect(bitmap)

                    // --- FOCUS LOCKING LOGIC ---
                    if (targetPersonId != -1) {
                        // If we have a target, check if they are still detected
                        val targetStillExists = detections.any { it.id == targetPersonId }
                        if (!targetStillExists) {
                            targetPersonId = -1 // Target lost, reset
                        }
                    }

                    if (targetPersonId == -1 && detections.isNotEmpty()) {
                        // If we don't have a target and people are detected, pick a new one
                        // We pick the one with the highest score as the new target
                        targetPersonId = detections.maxByOrNull { it.score }?.id ?: -1
                    }
                    // --- END OF LOGIC ---

                    runOnUiThread {
                        overlay.setResults(detections, smoothedFps, targetPersonId)
                        val command = overlay.getCommandText()
                        speak(command)
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity", "Analyzer error", e)
                } finally {
                    imageProxy.close()
                }
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
            } catch (e: Exception) {
                Log.e("MainActivity", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        tts.stop()
        tts.shutdown()
    }
}