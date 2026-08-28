package com.gamecontrol.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.gamecontrol.app.R
import com.gamecontrol.app.input.InputMapper
import com.google.android.material.button.MaterialButton

class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isShowing = false

    fun show() {
        if (isShowing) return

        if (!android.provider.Settings.canDrawOverlays(context)) return

        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }

        val btnToggle = overlayView?.findViewById<MaterialButton>(R.id.btnOverlayToggle)
        btnToggle?.setOnClickListener {
            val enabled = btnToggle.tag as? Boolean ?: true
            InputMapper.setMappingEnabled(!enabled)
            btnToggle.tag = !enabled
            btnToggle.text = if (enabled) "Mapping OFF" else "Toggle Mapping"
        }

        setupDrag(params)

        windowManager.addView(overlayView, params)
        isShowing = true
    }

    fun hide() {
        if (!isShowing || overlayView == null) return
        windowManager.removeView(overlayView)
        overlayView = null
        isShowing = false
    }

    fun updateDeviceStatus(name: String) {
        val tv = overlayView?.findViewById<TextView>(R.id.tvOverlayDevice)
        tv?.text = name
    }

    private fun setupDrag(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }
    }
}
