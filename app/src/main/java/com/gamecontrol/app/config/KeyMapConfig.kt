package com.gamecontrol.app.config

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class KeyMapEntry(
    val name: String = "",
    val keyCode: Int = 0,
    val action: String = "tap",
    val x: Float = 0f,
    val y: Float = 0f,
    val x2: Float = 0f,
    val y2: Float = 0f,
    val duration: Long = 50,
    val steps: List<MacroStep>? = null
)

data class MacroStep(
    val action: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val x2: Float = 0f,
    val y2: Float = 0f,
    val duration: Long = 50
)

data class KeyMapProfile(
    val name: String,
    val packageName: String,
    val mappings: List<KeyMapEntry>
)

object KeyMapConfig {

    const val TAG = "KeyMapConfig"
    private const val CONFIG_FILE = "keymap.json"

    private var configDir: File? = null
    private val gson = Gson()

    var mappings: List<KeyMapEntry> = getDefaultMappings()
        private set

    fun init(dir: File) {
        configDir = dir
        load()
    }

    fun load(): KeyMapConfig {
        val file = File(configDir, CONFIG_FILE)
        if (file.exists()) {
            try {
                val json = file.readText()
                val type = object : TypeToken<List<KeyMapEntry>>() {}.type
                mappings = gson.fromJson(json, type)
                Log.d(TAG, "Loaded ${mappings.size} mappings from $CONFIG_FILE")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load config", e)
                mappings = getDefaultMappings()
            }
        } else {
            mappings = getDefaultMappings()
            save()
        }
        return this
    }

    fun save() {
        val file = File(configDir, CONFIG_FILE)
        try {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(mappings))
            Log.d(TAG, "Saved ${mappings.size} mappings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config", e)
        }
    }

    fun updateMappings(newMappings: List<KeyMapEntry>) {
        mappings = newMappings
        save()
    }

    private fun getDefaultMappings(): List<KeyMapEntry> = listOf(
        KeyMapEntry("Fire", android.view.KeyEvent.KEYCODE_SPACE, "tap", 900f, 400f),
        KeyMapEntry("Aim", android.view.KeyEvent.KEYCODE_Q, "long_press", 540f, 400f, duration = 200),
        KeyMapEntry("Jump", android.view.KeyEvent.KEYCODE_E, "tap", 950f, 700f),
        KeyMapEntry("Reload", android.view.KeyEvent.KEYCODE_R, "tap", 100f, 750f),
        KeyMapEntry("Crouch", android.view.KeyEvent.KEYCODE_C, "tap", 150f, 400f),
        KeyMapEntry("Map", android.view.KeyEvent.KEYCODE_M, "tap", 50f, 50f),
        KeyMapEntry("Weapon1", android.view.KeyEvent.KEYCODE_1, "tap", 850f, 100f),
        KeyMapEntry("Weapon2", android.view.KeyEvent.KEYCODE_2, "tap", 850f, 200f),
        KeyMapEntry("Weapon3", android.view.KeyEvent.KEYCODE_3, "tap", 850f, 300f),
        KeyMapEntry("Move Up", android.view.KeyEvent.KEYCODE_W, "swipe", 200f, 500f, 200f, 400f, 100),
        KeyMapEntry("Move Down", android.view.KeyEvent.KEYCODE_S, "swipe", 200f, 500f, 200f, 600f, 100),
        KeyMapEntry("Move Left", android.view.KeyEvent.KEYCODE_A, "swipe", 200f, 500f, 100f, 500f, 100),
        KeyMapEntry("Move Right", android.view.KeyEvent.KEYCODE_D, "swipe", 200f, 500f, 300f, 500f, 100)
    )
}
