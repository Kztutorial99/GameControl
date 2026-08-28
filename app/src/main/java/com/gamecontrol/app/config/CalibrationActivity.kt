package com.gamecontrol.app.config

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalibrationActivity : AppCompatActivity() {

    private val markers = mutableListOf<Pair<Float, Float>>()
    private var selectedMarker = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        val container = FrameLayout(this)
        container.setBackgroundColor(Color.parseColor("#CC000000"))

        val tvInstruction = TextView(this).apply {
            text = "Tap anywhere to set coordinates\nTap X to cancel"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(32, 64, 32, 0)
        }
        container.addView(tvInstruction)

        val tvCoord = TextView(this).apply {
            setTextColor(Color.parseColor("#FF03DAC5"))
            textSize = 20f
            setPadding(32, 120, 32, 0)
        }
        container.addView(tvCoord)

        // Load existing mappings as markers
        val profile = ProfileManager.activeProfile
        profile?.mappings?.forEach { entry ->
            markers.add(Pair(entry.x, entry.y))
        }

        val touchView = object : View(this) {
            private val markerPaint = Paint().apply {
                color = Color.parseColor("#FF6200EE")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            private val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                isAntiAlias = true
            }
            private val linePaint = Paint().apply {
                color = Color.parseColor("#80FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                // Draw grid
                for (i in 0..10) {
                    val gx = width * i / 10f
                    canvas.drawLine(gx, 0f, gx, height.toFloat(), linePaint)
                    val gy = height * i / 10f
                    canvas.drawLine(0f, gy, width.toFloat(), gy, linePaint)
                }

                // Draw markers
                markers.forEachIndexed { idx, (mx, my) ->
                    val sx = mx / screenWidth * width
                    val sy = my / screenHeight * height
                    canvas.drawCircle(sx, sy, 20f, markerPaint)
                    val label = profile?.mappings?.getOrNull(idx)?.name ?: "${idx + 1}"
                    canvas.drawText(label, sx + 25f, sy + 8f, textPaint)
                }
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val touchX = event.x / width * screenWidth
                    val touchY = event.y / height * screenHeight
                    tvCoord.text = "Last tap: (${touchX.toInt()}, ${touchY.toInt()})"

                    val resultIntent = Intent().apply {
                        putExtra("x", touchX)
                        putExtra("y", touchY)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                    return true
                }
                return super.onTouchEvent(event)
            }
        }
        container.addView(touchView)

        // Cancel button
        val btnCancel = TextView(this).apply {
            text = "✕ Cancel"
            setTextColor(Color.parseColor("#FFF44336"))
            textSize = 16f
            setPadding(32, 32, 32, 32)
            setOnClickListener { finish() }
        }
        container.addView(btnCancel)

        setContentView(container)
    }
}
