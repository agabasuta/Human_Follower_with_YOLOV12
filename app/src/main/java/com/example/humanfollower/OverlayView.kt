package com.example.humanfollower

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // --- UPDATED PAINTS ---
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.GREEN // Default color for non-targets
    }

    private val targetBoxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.YELLOW // Highlight color for the target
    }

    private val textPaint = Paint().apply {
        textSize = 50f
        isAntiAlias = true
        color = Color.GREEN
    }

    private var detections: List<Detection> = emptyList()
    private var fps: Float = 0f
    private var command: String = "No person detected"
    private var targetId: Int = -1 // To know who to highlight

    fun setResults(list: List<Detection>, fps: Float, targetId: Int) {
        this.detections = list
        this.fps = fps
        this.targetId = targetId

        // Find the target person to compute command
        val targetPerson = detections.find { it.id == targetId }
        computeCommand(targetPerson)

        postInvalidate() // Redraw the view
    }

    fun getCommandText(): String = command

    private fun computeCommand(target: Detection?) {
        if (target == null) {
            command = "No person detected"
            return
        }

        val centerX = (target.x1 + target.x2) / 2f
        val deadZone = 0.1f // 10% deadzone
        command = when {
            centerX < 0.5f - deadZone -> "Move Left"
            centerX > 0.5f + deadZone -> "Move Right"
            else -> "Stay Centered"
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()

        for (d in detections) {
            val r = RectF(d.x1 * w, d.y1 * h, d.x2 * w, d.y2 * h)

            // --- Use the correct paint based on whether it's the target ---
            val currentPaint = if (d.id == targetId) targetBoxPaint else boxPaint
            c.drawRect(r, currentPaint)

            c.drawText(
                "ID:${d.id} ${"%.2f".format(d.score)}", // Simplified text
                r.left,
                r.top - 10f,
                textPaint
            )
        }

        val textW = textPaint.measureText(command)
        c.drawText(command, (w - textW) / 2f, h - 100f, textPaint)
        c.drawText("FPS: ${"%.1f".format(fps)}", 30f, 60f, textPaint)
    }
}