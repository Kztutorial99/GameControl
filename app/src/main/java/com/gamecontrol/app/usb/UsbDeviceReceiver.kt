package com.gamecontrol.app.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

class UsbDeviceReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "UsbDeviceReceiver"
        var onDeviceChanged: ((UsbDevice?, Boolean) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                Log.d(TAG, "Device connected: ${device?.deviceName}")
                onDeviceChanged?.invoke(device, true)
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                Log.d(TAG, "Device disconnected: ${device?.deviceName}")
                onDeviceChanged?.invoke(device, false)
            }
        }
    }
}
