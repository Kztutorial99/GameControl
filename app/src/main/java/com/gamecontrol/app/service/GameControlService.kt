package com.gamecontrol.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gamecontrol.app.R
import com.gamecontrol.app.input.InputMapper
import com.gamecontrol.app.input.TouchInjector
import com.gamecontrol.app.overlay.OverlayManager

class GameControlService : Service() {

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

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        Log.d(TAG, "Service created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        TouchInjector.init()
        InputMapper.init()

        overlayManager = OverlayManager(this)
        overlayManager?.show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        isRunning = false
        overlayManager?.hide()
        InputMapper.release()
        TouchInjector.release()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
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
        .setContentText("Keyboard & mouse mapping via Shizuku")
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
}
