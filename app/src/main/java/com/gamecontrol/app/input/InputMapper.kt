package com.gamecontrol.app.input

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.gamecontrol.app.config.KeyMapEntry
import com.gamecontrol.app.config.ProfileManager

object InputMapper {

    const val TAG = "InputMapper"

    private var isMappingEnabled = true

    fun init() {
        Log.d(TAG, "InputMapper initialized with ${ProfileManager.getMappings().size} mappings")
    }

    fun release() {
        // cleanup
    }

    fun reloadConfig() {
        // ProfileManager handles this
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        if (!isMappingEnabled || event == null) return false
        if (!TouchInjector.isAvailable()) return false

        val keyCode = event.keyCode
        val action = event.action

        val mapping = ProfileManager.getMappings().find { it.keyCode == keyCode } ?: return false

        Log.d(TAG, "Key event: code=$keyCode, action=$action, mapped to ${mapping.action}")

        return when (action) {
            KeyEvent.ACTION_DOWN -> executeAction(mapping)
            KeyEvent.ACTION_UP -> true
            else -> false
        }
    }

    fun handleMotionEvent(event: MotionEvent?): Boolean {
        if (!isMappingEnabled || event == null) return false
        if (!TouchInjector.isAvailable()) return false

        val source = event.source
        if (source != InputDevice.SOURCE_MOUSE) return false

        return false
    }

    private fun executeAction(mapping: KeyMapEntry): Boolean {
        return when (mapping.action) {
            "tap" -> {
                TouchInjector.injectTap(mapping.x, mapping.y)
                true
            }
            "swipe" -> {
                TouchInjector.injectSwipe(mapping.x, mapping.y, mapping.x2, mapping.y2, mapping.duration)
                true
            }
            "long_press" -> {
                TouchInjector.injectLongPress(mapping.x, mapping.y, mapping.duration)
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
        val steps = mapping.steps ?: return

        for (step in steps) {
            when (step.action) {
                "tap" -> TouchInjector.injectTap(step.x, step.y)
                "swipe" -> TouchInjector.injectSwipe(step.x, step.y, step.x2, step.y2, step.duration)
                "long_press" -> TouchInjector.injectLongPress(step.x, step.y, step.duration)
                "delay" -> Thread.sleep(step.duration)
            }
        }
    }

    fun setMappingEnabled(enabled: Boolean) {
        isMappingEnabled = enabled
    }
}
