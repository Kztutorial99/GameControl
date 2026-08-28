package com.gamecontrol.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.gamecontrol.app.config.KeyMapEntry
import com.gamecontrol.app.config.ProfileManager

class OverlayEditorView(context: Context) : View(context) {

    interface OnMarkerInteractionListener {
        fun onMarkerTapped(index: Int, entry: KeyMapEntry)
        fun onMarkerMoved(index: Int, x: Float, y: Float)
    }

    var listener: OnMarkerInteractionListener? = null
    var isEditMode = false

    private val screenW: Float
    private val screenH: Float

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        textSize = 18f
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private var draggedIndex = -1
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private val markerRadius = 40f

    init {
        val dm = context.resources.displayMetrics
        screenW = dm.widthPixels.toFloat()
        screenH = dm.heightPixels.toFloat()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isEditMode) return

        val mappings = ProfileManager.getMappings()
        val scaleX = width.toFloat() / screenW
        val scaleY = height.toFloat() / screenH

        mappings.forEachIndexed { index, entry ->
            val sx = entry.x * scaleX
            val sy = entry.y * scaleY

            // Color based on action type
            markerPaint.color = when (entry.action) {
                "tap" -> Color.parseColor("#80FF6200EE")
                "swipe" -> Color.parseColor("#8003DAC5")
                "long_press" -> Color.parseColor("#80FF9800")
                "macro" -> Color.parseColor("#80F44336")
                else -> Color.parseColor("#806200EE")
            }

            // Draw circle
            canvas.drawCircle(sx, sy, markerRadius, markerPaint)
            canvas.drawCircle(sx, sy, markerRadius, strokePaint)

            // Draw key label
            val label = entry.keyLabel.ifEmpty { "?" }
            val textW = textPaint.measureText(label)
            canvas.drawText(label, sx - textW / 2, sy + 8f, textPaint)

            // Draw name below
            val name = entry.name.ifEmpty { "Mapping $index" }
            val nameW = subTextPaint.measureText(name)
            canvas.drawText(name, sx - nameW / 2, sy + markerRadius + 20f, subTextPaint)

            // Draw swipe arrow
            if (entry.action == "swipe") {
                val ex = entry.x2 * scaleX
                val ey = entry.y2 * scaleY
                canvas.drawLine(sx, sy, ex, ey, strokePaint)
                canvas.drawCircle(ex, ey, 12f, markerPaint)
            }
        }

        // Grid overlay (subtle)
        if (isEditMode) {
            for (i in 1..9) {
                val gx = width * i / 10f
                canvas.drawLine(gx, 0f, gx, height.toFloat(), linePaint)
                val gy = height * i / 10f
                canvas.drawLine(0f, gy, width.toFloat(), gy, linePaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditMode) return false

        val scaleX = screenW / width.toFloat()
        val scaleY = screenH / height.toFloat()
        val rawX = event.x * scaleX
        val rawY = event.y * scaleY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if tapping on a marker
                val mappings = ProfileManager.getMappings()
                for (i in mappings.indices.reversed()) {
                    val entry = mappings[i]
                    val dx = rawX - entry.x
                    val dy = rawY - entry.y
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist <= markerRadius * (screenW / width)) {
                        draggedIndex = i
                        dragOffsetX = dx
                        dragOffsetY = dy
                        return true
                    }
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggedIndex >= 0) {
                    val newX = rawX - dragOffsetX
                    val newY = rawY - dragOffsetY
                    ProfileManager.updateMapping(draggedIndex,
                        ProfileManager.getMappings()[draggedIndex].apply {
                            x = newX.coerceIn(0f, screenW)
                            y = newY.coerceIn(0f, screenH)
                        }
                    )
                    invalidate()
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP -> {
                if (draggedIndex >= 0) {
                    val mappings = ProfileManager.getMappings()
                    val entry = mappings[draggedIndex]
                    val dx = rawX - entry.x - dragOffsetX
                    val dy = rawY - entry.y - dragOffsetY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    if (dist < 10f) {
                        // It's a tap, not a drag
                        listener?.onMarkerTapped(draggedIndex, entry)
                    } else {
                        listener?.onMarkerMoved(draggedIndex, entry.x, entry.y)
                    }
                    draggedIndex = -1
                    return true
                }
                return false
            }
        }
        return false
    }
}
