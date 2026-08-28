package com.gamecontrol.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.gamecontrol.app.config.KeyMapEntry
import com.gamecontrol.app.config.ProfileManager
import com.gamecontrol.app.input.InputMapper

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var toggleButton: TextView? = null
    private var toggleParams: WindowManager.LayoutParams? = null
    private var isToggleShowing = false

    private var configPanel: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var isPanelShowing = false

    private var editorView: OverlayEditorView? = null
    private var editorParams: WindowManager.LayoutParams? = null
    private var isEditorShowing = false

    private var pickOverlay: View? = null
    private var pickCancelBtn: View? = null
    private var isPickMode = false
    private var pickCallback: ((Float, Float) -> Unit)? = null

    private var dialogView: View? = null

    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        showToggleButton()
    }

    fun hide() {
        try { hideDialog() } catch (_: Exception) {}
        try { hidePickOverlay() } catch (_: Exception) {}
        try { hidePanel() } catch (_: Exception) {}
        try { hideEditor() } catch (_: Exception) {}
        try {
            toggleButton?.let { windowManager.removeView(it) }
            toggleButton = null
        } catch (_: Exception) {}
        isToggleShowing = false
    }

    private fun getScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    // ─── Toggle Button ───
    private fun showToggleButton() {
        if (isToggleShowing) return

        try {
            toggleButton = TextView(context).apply {
                text = "🎮"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#DD6200EE"))
                setPadding(28, 20, 28, 20)
                gravity = Gravity.CENTER
            }

            toggleParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 500
            }

            setupDrag(toggleButton!!, toggleParams!!)

            toggleButton!!.setOnClickListener {
                try {
                    if (isPanelShowing) {
                        hidePanel()
                        hideEditor()
                    } else {
                        showPanel()
                        showEditor()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OverlayManager", "Toggle error", e)
                }
            }

            windowManager.addView(toggleButton, toggleParams)
            isToggleShowing = true
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to show toggle button", e)
        }
    }

    // ─── Config Panel ───
    private fun showPanel() {
        if (isPanelShowing) return

        try {
            val panel = ScrollView(context).apply {
                setBackgroundColor(Color.parseColor("#F01A1A2E"))
                setPadding(20, 16, 20, 16)
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Header
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, 12)
            }
            header.addView(TextView(context).apply {
                text = "🎮 GameControl"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            header.addView(makeBtn("✕", "#FFF44336") {
                hidePanel()
                hideEditor()
            })
            container.addView(header)

            // Profile info
            container.addView(TextView(context).apply {
                text = "Profile: ${ProfileManager.activeProfile?.name ?: "Default"}"
                setTextColor(Color.parseColor("#B0B0B0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(0, 0, 0, 12)
            })

            // Mapping list
            val mappings = ProfileManager.getMappings()
            mappings.forEachIndexed { index, entry ->
                container.addView(makeMappingRow(index, entry))
            }

            // Action buttons
            val btnRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 0)
            }
            btnRow.addView(makeBtn("+ Add", "#FF6200EE") {
                enterPickMode { x, y ->
                    ProfileManager.addMapping(KeyMapEntry(
                        name = "Mapping ${mappings.size + 1}",
                        keyCode = 0, keyLabel = "?",
                        action = "tap", x = x, y = y
                    ))
                    hidePanel()
                    showPanel()
                    showEditor()
                }
            }, lp(1f, 4))
            btnRow.addView(makeBtn(if (isEditorShowing) "🔒" else "✏️", "#FF03DAC5") {
                editorView?.isEditMode = !(editorView?.isEditMode ?: false)
                editorView?.invalidate()
            }, lp(1f, 4))
            btnRow.addView(makeBtn("💾", "#FF4CAF50") {
                ProfileManager.save()
                InputMapper.reloadConfig()
                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
            }, lp(1f, 4))
            container.addView(btnRow)

            panel.addView(container)

            panelParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                600,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 10
                y = 200
            }

            setupDrag(panel, panelParams!!)
            windowManager.addView(panel, panelParams)
            configPanel = panel
            isPanelShowing = true
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to show panel", e)
        }
    }

    private fun hidePanel() {
        try {
            configPanel?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        configPanel = null
        isPanelShowing = false
    }

    // ─── Editor (markers) ───
    private fun showEditor() {
        if (isEditorShowing) return

        try {
            val (sw, sh) = getScreenSize()
            editorView = OverlayEditorView(context).apply {
                isEditMode = true
                listener = object : OverlayEditorView.OnMarkerInteractionListener {
                    override fun onMarkerTapped(index: Int, entry: KeyMapEntry) {
                        showMappingEditDialog(index, entry)
                    }
                    override fun onMarkerMoved(index: Int, x: Float, y: Float) {
                        ProfileManager.save()
                    }
                }
            }

            editorParams = WindowManager.LayoutParams(
                sw, sh,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            windowManager.addView(editorView, editorParams)
            isEditorShowing = true
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to show editor", e)
        }
    }

    private fun hideEditor() {
        try {
            editorView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        editorView = null
        isEditorShowing = false
    }

    // ─── Pick Mode ───
    private fun enterPickMode(callback: (Float, Float) -> Unit) {
        isPickMode = true
        pickCallback = callback

        try {
            val (sw, sh) = getScreenSize()

            pickOverlay = object : View(context) {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 40f
                    textAlign = android.graphics.Paint.Align.CENTER
                    setShadowLayer(8f, 0f, 0f, Color.BLACK)
                }

                override fun onDraw(canvas: android.graphics.Canvas) {
                    canvas.drawColor(Color.parseColor("#80000000"))
                    canvas.drawText("Tap anywhere to set position", width / 2f, height / 2f - 20f, paint)
                    paint.textSize = 24f
                    canvas.drawText("Tap ✕ to cancel", width / 2f, height / 2f + 30f, paint)
                }

                override fun onTouchEvent(event: MotionEvent): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val x = event.x / width * sw
                        val y = event.y / height * sh
                        pickCallback?.invoke(x, y)
                        hidePickOverlay()
                        return true
                    }
                    return false
                }
            }

            val pickP = WindowManager.LayoutParams(
                sw, sh,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            windowManager.addView(pickOverlay, pickP)

            pickCancelBtn = makeBtn("✕ Cancel", "#FFF44336") { hidePickOverlay() }
            val cancelP = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 120
            }
            windowManager.addView(pickCancelBtn, cancelP)

        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to enter pick mode", e)
            hidePickOverlay()
        }
    }

    private fun hidePickOverlay() {
        try { pickOverlay?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        try { pickCancelBtn?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        pickOverlay = null
        pickCancelBtn = null
        isPickMode = false
        pickCallback = null
    }

    // ─── Edit Dialog ───
    private fun showMappingEditDialog(index: Int, entry: KeyMapEntry) {
        hideEditor()

        try {
            val view = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 24)
                setBackgroundColor(Color.parseColor("#F01A1A2E"))
            }

            val etName = EditText(context).apply {
                hint = "Name"
                setText(entry.name)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.parseColor("#80FFFFFF"))
            }
            view.addView(etName)

            var capturedCode = entry.keyCode
            var capturedLabel = entry.keyLabel

            val tvKey = TextView(context).apply {
                text = if (entry.keyLabel.isNotEmpty()) "Key: ${entry.keyLabel}" else "Key: (press any key)"
                setTextColor(Color.parseColor("#03DAC5"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, 16, 0, 8)
                isFocusableInTouchMode = true
                isFocusable = true
            }
            tvKey.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    capturedCode = keyCode
                    capturedLabel = android.view.KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "")
                    tvKey.text = "Key: $capturedLabel"
                    true
                } else false
            }
            view.addView(tvKey)

            view.addView(TextView(context).apply {
                text = "Position: (${entry.x.toInt()}, ${entry.y.toInt()})"
                setTextColor(Color.parseColor("#B0B0B0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 4, 0, 8)
            })

            // Action type
            var selectedAction = entry.action
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            val actions = listOf("tap" to "Tap", "swipe" to "Swipe", "long_press" to "Hold")
            val actionBtns = mutableListOf<TextView>()
            actions.forEach { (key, label) ->
                val btn = TextView(context).apply {
                    text = label
                    setTextColor(if (entry.action == key) Color.WHITE else Color.parseColor("#B0B0B0"))
                    setBackgroundColor(if (entry.action == key) Color.parseColor("#FF6200EE") else Color.parseColor("#333333"))
                    setPadding(16, 8, 16, 8)
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        selectedAction = key
                        actionBtns.forEach { b ->
                            b.setTextColor(Color.parseColor("#B0B0B0"))
                            b.setBackgroundColor(Color.parseColor("#333333"))
                        }
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.parseColor("#FF6200EE"))
                    }
                }
                actionBtns.add(btn)
                actionRow.addView(btn, lp(1f, 4))
            }
            view.addView(actionRow)

            // Buttons
            val btnRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 0)
            }
            btnRow.addView(makeBtn("Save", "#FF4CAF50") {
                ProfileManager.updateMapping(index, KeyMapEntry(
                    name = etName.text.toString().ifEmpty { "Unnamed" },
                    keyCode = capturedCode, keyLabel = capturedLabel,
                    action = selectedAction,
                    x = entry.x, y = entry.y,
                    x2 = entry.x2, y2 = entry.y2,
                    duration = entry.duration
                ))
                hideDialog()
                showEditor()
            }, lp(1f, 4))
            btnRow.addView(makeBtn("Delete", "#FFF44336") {
                ProfileManager.removeMapping(index)
                hideDialog()
                showEditor()
            }, lp(1f, 4))
            btnRow.addView(makeBtn("Cancel", "#555555") {
                hideDialog()
                showEditor()
            }, lp(1f, 4))
            view.addView(btnRow)

            val dp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                dimAmount = 0.5f
            }

            windowManager.addView(view, dp)
            dialogView = view
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to show dialog", e)
        }
    }

    private fun hideDialog() {
        try { dialogView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        dialogView = null
    }

    // ─── Helpers ───
    private fun lp(weight: Float, margin: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
            marginEnd = margin
            marginStart = margin
        }
    }

    private fun makeBtn(text: String, color: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(color))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(16, 10, 16, 10)
            gravity = Gravity.CENTER
            setOnClickListener { try { onClick() } catch (_: Exception) {} }
        }
    }

    private fun makeMappingRow(index: Int, entry: KeyMapEntry): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 6, 8, 6)
            setBackgroundColor(if (index % 2 == 0) Color.parseColor("#16213E") else Color.parseColor("#1A1A2E"))

            addView(TextView(context).apply {
                text = entry.keyLabel.ifEmpty { "?" }
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#FF6200EE"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setPadding(8, 4, 8, 4)
                gravity = Gravity.CENTER
                minWidth = 60
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = lp(1f, 8)
                addView(TextView(context).apply {
                    text = entry.name.ifEmpty { "Unnamed" }
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                })
                addView(TextView(context).apply {
                    text = "${entry.getActionLabel()} (${entry.x.toInt()}, ${entry.y.toInt()})"
                    setTextColor(Color.parseColor("#B0B0B0"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                })
            })
        }
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 15 || Math.abs(dy) > 15) {
                        isDragging = true
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        try { windowManager.updateViewLayout(v, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        v.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
