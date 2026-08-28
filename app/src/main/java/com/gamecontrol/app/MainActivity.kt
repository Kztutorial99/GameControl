package com.gamecontrol.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gamecontrol.app.config.CalibrationActivity
import com.gamecontrol.app.config.KeyMapEditorActivity
import com.gamecontrol.app.config.ProfileListActivity
import com.gamecontrol.app.config.ProfileManager
import com.gamecontrol.app.input.InputMapper
import com.gamecontrol.app.overlay.OverlayManager
import com.gamecontrol.app.service.GameControlService
import com.gamecontrol.app.usb.UsbDeviceManager
import com.google.android.material.button.MaterialButton
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleService: MaterialButton
    private lateinit var btnShizuku: MaterialButton
    private lateinit var btnOverlayPermission: MaterialButton
    private lateinit var btnProfiles: MaterialButton
    private lateinit var btnKeymapEditor: MaterialButton
    private lateinit var btnCalibrate: MaterialButton
    private lateinit var tvServiceStatus: TextView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var tvDeviceInfo: TextView

    private var usbDeviceManager: UsbDeviceManager? = null
    private var overlayManager: OverlayManager? = null

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku permission granted!", Toast.LENGTH_SHORT).show()
            updateShizukuStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ProfileManager.init(this)

        bindViews()
        setupListeners()

        usbDeviceManager = UsbDeviceManager(this)
        overlayManager = OverlayManager(this)

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateDeviceStatus()
        updateShizukuStatus()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        super.onDestroy()
    }

    private fun bindViews() {
        btnToggleService = findViewById(R.id.btnToggleService)
        btnShizuku = findViewById(R.id.btnShizuku)
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission)
        btnProfiles = findViewById(R.id.btnProfiles)
        btnKeymapEditor = findViewById(R.id.btnKeymapEditor)
        btnCalibrate = findViewById(R.id.btnCalibrate)
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus)
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo)
    }

    private fun setupListeners() {
        btnToggleService.setOnClickListener { toggleService() }

        btnShizuku.setOnClickListener {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Install Shizuku app first and activate via ADB", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(0)
            } else {
                Toast.makeText(this, "Shizuku already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnOverlayPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnProfiles.setOnClickListener {
            startActivity(Intent(this, ProfileListActivity::class.java))
        }

        btnKeymapEditor.setOnClickListener {
            startActivity(Intent(this, KeyMapEditorActivity::class.java))
        }

        btnCalibrate.setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }
    }

    private fun toggleService() {
        if (GameControlService.isRunning) {
            stopService(Intent(this, GameControlService::class.java))
        } else {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Grant Shizuku permission first", Toast.LENGTH_LONG).show()
                return
            }
            startForegroundService(Intent(this, GameControlService::class.java))
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
            tvDeviceInfo.text = devices.joinToString("\n") { "${it.name} [kb=${it.isKeyboard}, mouse=${it.isMouse}]" }
        } else {
            tvDeviceStatus.text = getString(R.string.status_disconnected)
            tvDeviceStatus.setTextColor(getColor(R.color.status_red))
            tvDeviceInfo.text = "Connect a keyboard or mouse via OTG"
        }
    }

    private fun updateShizukuStatus() {
        when {
            !Shizuku.pingBinder() -> {
                btnShizuku.text = "Install & Activate Shizuku"
                btnShizuku.isEnabled = true
            }
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> {
                btnShizuku.text = "Grant Shizuku Permission"
                btnShizuku.isEnabled = true
            }
            else -> {
                btnShizuku.text = "Shizuku ✓ Connected"
                btnShizuku.isEnabled = false
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (GameControlService.isRunning) {
            InputMapper.handleKeyEvent(event)
        }
        return super.onKeyDown(keyCode, event)
    }
}
