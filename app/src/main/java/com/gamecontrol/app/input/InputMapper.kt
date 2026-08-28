package com.gamecontrol.app.input

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.gamecontrol.app.config.KeyMapConfig
import com.gamecontrol.app.config.KeyMapEntry
import com.gamecontrol.app.service.GameControlService

object InputMapper {

    const val TAG = "InputMapper"

    private var service: GameControlService? = null
    private var keyMapConfig = KeyMapConfig.load()
    private var isMappingEnabled = true

    fun init(svc: GameControlService) {
        service = svc
        Log.d(TAG, "InputMapper initialized with ${keyMapConfig.mappings.size} mappings")
    }

    fun release() {
        service = null
    }

    fun reloadConfig() {
        keyMapConfig = KeyMapConfig.load()
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        if (!isMappingEnabled || event == null || service == null) return false

        val keyCode = event.keyCode
        val action = event.action

        val mapping = keyMapConfig.mappings.find { it.keyCode == keyCode } ?: return false

        Log.d(TAG, "Key event: code=$keyCode, action=$action, mapped to ${mapping.action}")

        return when (action) {
            KeyEvent.ACTION_DOWN -> executeAction(mapping)
            KeyEvent.ACTION_UP -> true
            else -> false
        }
    }

    fun handleMotionEvent(event: MotionEvent?): Boolean {
        if (!isMappingEnabled || event == null || service == null) return false

        val source = event.source
        if (source != InputDevice.SOURCE_MOUSE) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val mapping = keyMapConfig.mappings.find { it.action == "aim" }
                if (mapping != null) {
                    service?.dispatchGesture(x, y)
                }
            }
        }

        return false
    }

    private fun executeAction(mapping: KeyMapEntry): Boolean {
        val svc = service ?: return false

        return when (mapping.action) {
            "tap" -> {
                svc.dispatchGesture(mapping.x, mapping.y)
                true
            }
            "swipe" -> {
                svc.dispatchSwipe(mapping.x, mapping.y, mapping.x2, mapping.y2, mapping.duration)
                true
            }
            "long_press" -> {
                svc.dispatchLongPress(mapping.x, mapping.y, mapping.duration)
                true
            }
            "macro" -> {
                executeMacro(mapping)
                true
            }
            else -> false
        }
    }

    private fun executeMacro(mapping: KeyMapEntry) {
        val svc = service ?: return
        val steps = mapping.steps ?: return

        for (step in steps) {
            when (step.action) {
                "tap" -> svc.dispatchGesture(step.x, step.y)
                "swipe" -> svc.dispatchSwipe(step.x, step.y, step.x2, step.y2, step.duration)
                "delay" -> Thread.sleep(step.duration)
            }
        }
    }

    fun setMappingEnabled(enabled: Boolean) {
        isMappingEnabled = enabled
    }
}
