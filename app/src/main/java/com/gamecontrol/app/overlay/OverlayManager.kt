package com.gamecontrol.app.overlay

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.gamecontrol.app.config.KeyMapEntry
import com.gamecontrol.app.config.ProfileManager
import com.gamecontrol.app.input.InputMapper

class OverlayManager(
    private val context: Context,
    private val onAddMappingRequest: (() -> Unit)? = null
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Floating toggle button
    private var toggleButton: TextView? = null
    private var toggleParams: WindowManager.LayoutParams? = null
    private var isToggleShowing = false

    // Config panel
    private var configPanel: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var isPanelShowing = false

    // Editor overlay (markers)
    private var editorView: OverlayEditorView? = null
    private var editorParams: WindowManager.LayoutParams? = null
    private var isEditorShowing = false

    // Pick mode
    private var isPickMode = false
    private var pickCallback: ((Float, Float) -> Unit)? = null

    // Pick mode overlay
    private var pickOverlay: View? = null
    private var pickParams: WindowManager.LayoutParams? = null

    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        showToggleButton()
    }

    fun hide() {
        hidePanel()
        hideEditor()
        hidePickOverlay()
        toggleButton?.let {
            windowManager.removeView(it)
            toggleButton = null
        }
        isToggleShowing = false
    }

    // ─── Toggle Button ───
    private fun showToggleButton() {
        if (isToggleShowing) return

        toggleButton = TextView(context).apply {
            text = "🎮"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC6200EE"))
            setPadding(24, 16, 24, 16)
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
            y = 400
        }

        setupDrag(toggleButton!!, toggleParams!!) { }

        toggleButton!!.setOnClickListener {
            if (isPanelShowing) {
                hidePanel()
                hideEditor()
            } else {
                showPanel()
                showEditor()
            }
        }

        windowManager.addView(toggleButton, toggleParams)
        isToggleShowing = true
    }

    // ─── Config Panel ───
    private fun showPanel() {
        if (isPanelShowing) return

        configPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F01A1A2E"))
            setPadding(20, 16, 20, 16)
            minimumWidth = 500
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(context).apply {
            text = "GameControl"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnHide = createButton("✕", Color.parseColor("#FFF44336")) {
            hidePanel()
            hideEditor()
        }
        header.addView(title)
        header.addView(btnHide)
        configPanel!!.addView(header)

        // Profile name
        val tvProfile = TextView(context).apply {
            text = "Profile: ${ProfileManager.activeProfile?.name ?: "Default"}"
            setTextColor(Color.parseColor("#B0B0B0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, 4, 0, 8)
        }
        configPanel!!.addView(tvProfile)

        // Scrollable mapping list
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
            )
        }
        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val mappings = ProfileManager.getMappings()
        mappings.forEachIndexed { index, entry ->
            val row = createMappingRow(index, entry)
            listContainer.addView(row)
        }
        scrollView.addView(listContainer)
        configPanel!!.addView(scrollView)

        // Action buttons
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }

        val btnAdd = createButton("+ Add", Color.parseColor("#FF6200EE")) {
            enterPickMode { x, y ->
                val newEntry = KeyMapEntry(
                    name = "Mapping ${mappings.size + 1}",
                    keyCode = 0,
                    keyLabel = "?",
                    action = "tap",
                    x = x,
                    y = y
                )
                ProfileManager.addMapping(newEntry)
                hidePanel()
                showPanel() // refresh
                showEditor()
            }
        }

        val btnToggleEdit = createButton(
            if (isEditorShowing) "🔒 Lock" else "✏️ Edit",
            Color.parseColor("#FF03DAC5")
        ) {
            editorView?.isEditMode = !(editorView?.isEditMode ?: false)
            editorView?.invalidate()
            hidePanel()
            showPanel()
        }

        val btnSave = createButton("💾", Color.parseColor("#FF4CAF50")) {
            ProfileManager.save()
            InputMapper.reloadConfig()
            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
        }

        btnRow.addView(btnAdd, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 })
        btnRow.addView(btnToggleEdit, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) })
        btnRow.addView(btnSave, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 4 })
        configPanel!!.addView(btnRow)

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }

        setupDrag(configPanel!!, panelParams!!) { }

        windowManager.addView(configPanel, panelParams)
        isPanelShowing = true
    }

    private fun hidePanel() {
        configPanel?.let {
            windowManager.removeView(it)
            configPanel = null
        }
        isPanelShowing = false
    }

    // ─── Editor (markers) ───
    private fun showEditor() {
        if (isEditorShowing) return

        val dm = context.resources.displayMetrics
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
            dm.widthPixels,
            dm.heightPixels,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(editorView, editorParams)
        isEditorShowing = true
    }

    private fun hideEditor() {
        editorView?.let {
            windowManager.removeView(it)
            editorView = null
        }
        isEditorShowing = false
    }

    // ─── Pick Mode ───
    private fun enterPickMode(callback: (Float, Float) -> Unit) {
        isPickMode = true
        pickCallback = callback

        val dm = context.resources.displayMetrics

        pickOverlay = object : View(context) {
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(8f, 0f, 0f, Color.BLACK)
            }
            val bgPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#80000000")
            }

            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
                canvas.drawText("Tap anywhere to set position", width / 2f, height / 2f - 30f, textPaint)
                canvas.drawText("Tap ✕ to cancel", width / 2f, height / 2f + 30f, textPaint.apply { textSize = 28f })
            }

            override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    val screenW = dm.widthPixels.toFloat()
                    val screenH = dm.heightPixels.toFloat()
                    val x = event.x / width * screenW
                    val y = event.y / height * screenH
                    pickCallback?.invoke(x, y)
                    hidePickOverlay()
                    return true
                }
                return false
            }
        }

        pickParams = WindowManager.LayoutParams(
            dm.widthPixels,
            dm.heightPixels,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(pickOverlay, pickParams)

        // Cancel button on top
        val btnCancel = TextView(context).apply {
            text = "✕ Cancel"
            setTextColor(Color.parseColor("#FFF44336"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(24, 12, 24, 12)
            setBackgroundColor(Color.parseColor("#CC000000"))
            setOnClickListener { hidePickOverlay() }
        }
        val cancelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
        }
        // We add cancel as a child conceptually but as separate overlay
        windowManager.addView(btnCancel, cancelParams)
        pickOverlay!!.tag = btnCancel // store for cleanup
    }

    private fun hidePickOverlay() {
        pickOverlay?.let {
            windowManager.removeView(it)
            (it.tag as? View)?.let { btn -> windowManager.removeView(btn) }
            pickOverlay = null
        }
        isPickMode = false
        pickCallback = null
    }

    // ─── Edit Dialog (via overlay) ───
    private fun showMappingEditDialog(index: Int, entry: KeyMapEntry) {
        // We need to temporarily allow focus for dialog
        hideEditor()

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

        val tvKey = TextView(context).apply {
            text = if (entry.keyLabel.isNotEmpty()) "Key: ${entry.keyLabel}" else "Key: (press any key)"
            setTextColor(Color.parseColor("#03DAC5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 16, 0, 8)
            isFocusableInTouchMode = true
            isFocusable = true
        }
        var capturedCode = entry.keyCode
        var capturedLabel = entry.keyLabel
        tvKey.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                capturedCode = keyCode
                capturedLabel = android.view.KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "")
                tvKey.text = "Key: $capturedLabel"
                true
            } else false
        }
        view.addView(tvKey)

        val tvCoords = TextView(context).apply {
            text = "Position: (${entry.x.toInt()}, ${entry.y.toInt()})"
            setTextColor(Color.parseColor("#B0B0B0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 4, 0, 8)
        }
        view.addView(tvCoords)

        // Action type buttons
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        var selectedAction = entry.action
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
            actionRow.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 })
        }
        view.addView(actionRow)

        // Show as overlay dialog
        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.5f
        }

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 0)
        }

        val btnSave = TextView(context).apply {
            text = "Save"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF4CAF50"))
            setPadding(32, 12, 32, 12)
            gravity = Gravity.CENTER
        }
        val btnDelete = TextView(context).apply {
            text = "Delete"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FFF44336"))
            setPadding(32, 12, 32, 12)
            gravity = Gravity.CENTER
        }
        val btnCancel = TextView(context).apply {
            text = "Cancel"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#555555"))
            setPadding(32, 12, 32, 12)
            gravity = Gravity.CENTER
        }

        view.addView(btnRow)
        btnRow.addView(btnSave, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 })
        btnRow.addView(btnDelete, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) })
        btnRow.addView(btnCancel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 4 })

        windowManager.addView(view, dialogParams)

        btnSave.setOnClickListener {
            ProfileManager.updateMapping(index, KeyMapEntry(
                name = etName.text.toString().ifEmpty { "Unnamed" },
                keyCode = capturedCode,
                keyLabel = capturedLabel,
                action = selectedAction,
                x = entry.x,
                y = entry.y,
                x2 = entry.x2,
                y2 = entry.y2,
                duration = entry.duration
            ))
            windowManager.removeView(view)
            showEditor()
        }

        btnDelete.setOnClickListener {
            ProfileManager.removeMapping(index)
            windowManager.removeView(view)
            showEditor()
        }

        btnCancel.setOnClickListener {
            windowManager.removeView(view)
            showEditor()
        }
    }

    // ─── Helpers ───
    private fun createButton(text: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(16, 10, 16, 10)
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun createMappingRow(index: Int, entry: KeyMapEntry): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 6, 8, 6)
            setBackgroundColor(if (index % 2 == 0) Color.parseColor("#16213E") else Color.parseColor("#1A1A2E"))

            val tvKey = TextView(context).apply {
                text = entry.keyLabel.ifEmpty { "?" }
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#FF6200EE"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setPadding(8, 4, 8, 4)
                gravity = Gravity.CENTER
                minWidth = 60
            }
            addView(tvKey)

            val info = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 8 }
            }
            info.addView(TextView(context).apply {
                text = entry.name.ifEmpty { "Unnamed" }
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })
            info.addView(TextView(context).apply {
                text = "${entry.getActionLabel()} (${entry.x.toInt()}, ${entry.y.toInt()})"
                setTextColor(Color.parseColor("#B0B0B0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            })
            addView(info)
        }
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams, onDragEnd: () -> Unit) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
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
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                    } else {
                        onDragEnd()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
