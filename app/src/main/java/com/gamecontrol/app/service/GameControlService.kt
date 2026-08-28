package com.gamecontrol.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.gamecontrol.app.R
import com.gamecontrol.app.input.InputMapper
import com.gamecontrol.app.overlay.OverlayManager

class GameControlService : AccessibilityService() {

    companion object {
        const val TAG = "GameControlService"
        const val CHANNEL_ID = "game_control_channel"
        const val NOTIFICATION_ID = 1001

        var instance: GameControlService? = null
            private set
        var isRunning = false
            private set
    }

    private var overlayManager: OverlayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Log.d(TAG, "Service connected")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        overlayManager = OverlayManager(this)
        overlayManager?.show()

        InputMapper.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // not used for input mapping
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        isRunning = false
        overlayManager?.hide()
        InputMapper.release()
        Log.d(TAG, "Service unbound")
        return super.onUnbind(intent)
    }

    fun dispatchGesture(x: Float, y: Float, duration: Long = 50) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 200) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    fun dispatchLongPress(x: Float, y: Float, duration: Long = 500) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("GameControl Active")
        .setContentText("Keyboard & mouse mapping enabled")
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
}
