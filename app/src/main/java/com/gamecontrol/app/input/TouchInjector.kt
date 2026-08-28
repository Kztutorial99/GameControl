package com.gamecontrol.app.input

import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

object TouchInjector {

    const val TAG = "TouchInjector"

    private var inputManager: Any? = null
    private var injectMethod: java.lang.reflect.Method? = null
    private var isReady = false

    private val SHIZUKU_BINDER_REQUEST_CODE = 1001

    fun init() {
        if (!Shizuku.pingBinder()) {
            Log.w(TAG, "Shizuku not available")
            return
        }

        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Shizuku permission not granted")
            requestPermission()
            return
        }

        setupInjection()
    }

    fun release() {
        inputManager = null
        injectMethod = null
        isReady = false
    }

    private fun requestPermission() {
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_BINDER_REQUEST_CODE) {
                if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    setupInjection()
                } else {
                    Log.e(TAG, "Shizuku permission denied")
                }
            }
        }
        Shizuku.requestPermission(SHIZUKU_BINDER_REQUEST_CODE)
    }

    private fun setupInjection() {
        try {
            val binder: IBinder = ShizukuBinderWrapper(
                Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String::class.java)
                    .invoke(null, "input") as IBinder
            )

            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            inputManager = asInterface.invoke(null, binder)

            injectMethod = inputManager!!.javaClass.getMethod(
                "injectInputEvent",
                MotionEvent::class.java,
                Int::class.javaPrimitiveType
            )

            isReady = true
            Log.d(TAG, "Shizuku input injection ready")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup injection", e)
            isReady = false
        }
    }

    fun isAvailable(): Boolean = isReady

    fun injectTap(x: Float, y: Float) {
        if (!isReady) return
        try {
            val now = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 1, 1.0f, 1.0f, 0, 0.0f, 0.0f, 0)
            down.source = InputDevice.SOURCE_TOUCHSCREEN
            val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 1, 1.0f, 1.0f, 0, 0.0f, 0.0f, 0)
            up.source = InputDevice.SOURCE_TOUCHSCREEN

            injectMethod?.invoke(inputManager, down, 2) // INJECT_MODE_ASYNC
            injectMethod?.invoke(inputManager, up, 2)

            down.recycle()
            up.recycle()

            Log.d(TAG, "Tap injected at ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject tap", e)
        }
    }

    fun injectSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 200) {
        if (!isReady) return
        try {
            val now = SystemClock.uptimeMillis()
            val steps = 10
            val stepDuration = durationMs / steps

            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, startX, startY, 1, 1.0f, 1.0f, 0, 0.0f, 0.0f, 0)
            down.source = InputDevice.SOURCE_TOUCHSCREEN
            injectMethod?.invoke(inputManager, down, 2)
            down.recycle()

            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val cx = startX + (endX - startX) * t
                val cy = startY + (endY - startY) * t
                val move = MotionEvent.obtain(now, now + stepDuration * i, MotionEvent.ACTION_MOVE, cx, cy, 1, 1.0f, 1.0f, 0, 0.0f, 0.0f, 0)
                move.source = InputDevice.SOURCE_TOUCHSCREEN
                injectMethod?.invoke(inputManager, move, 2)
                move.recycle()
                Thread.sleep(stepDuration)
            }

            val up = MotionEvent.obtain(now, now + durationMs, MotionEvent.ACTION_UP, endX, endY, 1, 1.0f, 1.0f, 0, 0.0f, 0.0f, 0)
            up.source = InputDevice.SOURCE_TOUCHSCREEN
            injectMethod?.invoke(inputManager, up, 2)
            up.recycle()

            Log.d(TAG, "Swipe injected ($startX,$startY) → ($endX,$endY)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject swipe", e)
        }
    }

    fun injectLongPress(x: Float, y: Float, durationMs: Long = 500) {
        if (!isReady) return
        try {
            val now = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 1, 1.0f, 1.0f, 0, 0.0f, 0.0f, 0)
            down.source = InputDevice.SOURCE_TOUCHSCREEN
            injectMethod?.invoke(inputManager, down, 2)
            down.recycle()

            Thread.sleep(durationMs)

            val up = MotionEvent.obtain(now, now + durationMs, MotionEvent.ACTION_UP, x, y, 1, 1.0f, 1.0f, 0, 0.0f, 0.0f, 0)
            up.source = InputDevice.SOURCE_TOUCHSCREEN
            injectMethod?.invoke(inputManager, up, 2)
            up.recycle()

            Log.d(TAG, "Long press injected at ($x, $y) for ${durationMs}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject long press", e)
        }
    }
}
