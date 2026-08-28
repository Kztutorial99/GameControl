package com.gamecontrol.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gamecontrol.app.input.InputMapper
import com.gamecontrol.app.overlay.OverlayManager
import com.gamecontrol.app.service.GameControlService
import com.gamecontrol.app.usb.UsbDeviceManager
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleService: MaterialButton
    private lateinit var btnAccessibility: MaterialButton
    private lateinit var btnOverlayPermission: MaterialButton
    private lateinit var btnConfigure: MaterialButton
    private lateinit var tvServiceStatus: TextView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var tvDeviceInfo: TextView

    private var usbDeviceManager: UsbDeviceManager? = null
    private var overlayManager: OverlayManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupListeners()

        usbDeviceManager = UsbDeviceManager(this)
        overlayManager = OverlayManager(this)
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateDeviceStatus()
    }

    private fun bindViews() {
        btnToggleService = findViewById(R.id.btnToggleService)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission)
        btnConfigure = findViewById(R.id.btnConfigure)
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus)
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo)
    }

    private fun setupListeners() {
        btnToggleService.setOnClickListener {
            toggleService()
        }

        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable GameControl in Accessibility settings", Toast.LENGTH_LONG).show()
        }

        btnOverlayPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnConfigure.setOnClickListener {
            Toast.makeText(this, "Key mapping config coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleService() {
        if (GameControlService.isRunning) {
            GameControlService.instance?.stopSelf()
        } else {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Enable Accessibility Service first", Toast.LENGTH_LONG).show()
                return
            }
            val intent = Intent(this, GameControlService::class.java)
            startForegroundService(intent)
        }
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        if (GameControlService.isRunning) {
            tvServiceStatus.text = getString(R.string.status_service_running)
            tvServiceStatus.setTextColor(getColor(R.color.status_green))
            btnToggleService.text = getString(R.string.btn_stop_service)
        } else {
            tvServiceStatus.text = getString(R.string.status_service_stopped)
            tvServiceStatus.setTextColor(getColor(R.color.status_red))
            btnToggleService.text = getString(R.string.btn_start_service)
        }
    }

    private fun updateDeviceStatus() {
        val devices = usbDeviceManager?.getConnectedInputDevices() ?: emptyList()
        if (devices.isNotEmpty()) {
            tvDeviceStatus.text = getString(R.string.status_connected)
            tvDeviceStatus.setTextColor(getColor(R.color.status_green))
            tvDeviceInfo.text = devices.joinToString("\n") { it.name }
        } else {
            tvDeviceStatus.text = getString(R.string.status_disconnected)
            tvDeviceStatus.setTextColor(getColor(R.color.status_red))
            tvDeviceInfo.text = "Connect a keyboard or mouse via OTG"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return prefString.contains("${packageName}/${GameControlService::class.java.name}")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (GameControlService.isRunning) {
            InputMapper.handleKeyEvent(event)
        }
        return super.onKeyDown(keyCode, event)
    }
}
