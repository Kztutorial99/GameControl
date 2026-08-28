package com.gamecontrol.app.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log

object GestureHelper {

    const val TAG = "GestureHelper"

    fun createTap(x: Float, y: Float, duration: Long = 50): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun createSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 200): GestureDescription {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun createLongPress(x: Float, y: Float, duration: Long = 500): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun createMultiTap(points: List<Pair<Float, Float>>, duration: Long = 50): GestureDescription {
        val builder = GestureDescription.Builder()
        for ((x, y) in points) {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration)
            builder.addStroke(stroke)
        }
        return builder.build()
    }
}
