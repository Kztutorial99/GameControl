package com.gamecontrol.app.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.InputDevice

class UsbDeviceManager(private val context: Context) {

    companion object {
        const val TAG = "UsbDeviceManager"
    }

    data class DeviceInfo(
        val name: String,
        val vendorId: Int,
        val productId: Int,
        val isKeyboard: Boolean,
        val isMouse: Boolean,
        val isGamepad: Boolean,
        val sources: Int
    )

    fun getConnectedInputDevices(): List<DeviceInfo> {
        val devices = mutableListOf<DeviceInfo>()

        val inputDeviceIds = InputDevice.getDeviceIds()
        for (id in inputDeviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue

            val sources = device.sources
            val isKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
            val isMouse = (sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD

            if (isKeyboard || isMouse || isGamepad) {
                devices.add(
                    DeviceInfo(
                        name = device.name ?: "Unknown Device",
                        vendorId = device.vendorId,
                        productId = device.productId,
                        isKeyboard = isKeyboard,
                        isMouse = isMouse,
                        isGamepad = isGamepad,
                        sources = sources
                    )
                )
                Log.d(TAG, "Found device: ${device.name} [kb=$isKeyboard, mouse=$isMouse, pad=$isGamepad]")
            }
        }

        return devices
    }

    fun getUsbDevices(): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.values.toList()
    }

    fun registerUsbReceiver(callback: (UsbDevice?, Boolean) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        Log.d(TAG, "USB attached: ${device?.deviceName}")
                        callback(device, true)
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        Log.d(TAG, "USB detached: ${device?.deviceName}")
                        callback(device, false)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(receiver, filter)
        return receiver
    }
}
