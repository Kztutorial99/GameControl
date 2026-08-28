package com.gamecontrol.app.config

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class KeyMapEntry(
    var name: String = "",
    var keyCode: Int = 0,
    var keyLabel: String = "",
    var action: String = "tap",
    var x: Float = 0f,
    var y: Float = 0f,
    var x2: Float = 0f,
    var y2: Float = 0f,
    var duration: Long = 50,
    var steps: List<MacroStep>? = null
) {
    fun getActionLabel(): String = when (action) {
        "tap" -> "Tap"
        "swipe" -> "Swipe"
        "long_press" -> "Long Press"
        "macro" -> "Macro"
        else -> action
    }

    fun getCoordinateText(): String = when (action) {
        "tap", "long_press" -> "(${x.toInt()}, ${y.toInt()})"
        "swipe" -> "(${x.toInt()}, ${y.toInt()}) → (${x2.toInt()}, ${y2.toInt()})"
        else -> "—"
    }
}

data class MacroStep(
    var action: String,
    var x: Float = 0f,
    var y: Float = 0f,
    var x2: Float = 0f,
    var y2: Float = 0f,
    var duration: Long = 50
)

data class KeyMapProfile(
    var name: String = "Default",
    var packageName: String = "",
    var mappings: MutableList<KeyMapEntry> = mutableListOf()
)

object ProfileManager {

    const val TAG = "ProfileManager"
    private const val PROFILES_DIR = "profiles"
    private const val ACTIVE_PROFILE_FILE = "active_profile.txt"

    private var configDir: File? = null
    private val gson = Gson()

    var profiles = mutableListOf<KeyMapProfile>()
        private set
    var activeProfile: KeyMapProfile? = null
        private set

    fun init(context: Context) {
        configDir = File(context.filesDir, PROFILES_DIR)
        configDir?.mkdirs()
        loadAll()
        if (profiles.isEmpty()) {
            profiles.add(getDefaultProfile())
            saveAll()
        }
        loadActiveProfile(context)
    }

    fun loadAll() {
        profiles.clear()
        val dir = configDir ?: return
        dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val json = file.readText()
                val profile = gson.fromJson(json, KeyMapProfile::class.java)
                profiles.add(profile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load profile: ${file.name}", e)
            }
        }
        Log.d(TAG, "Loaded ${profiles.size} profiles")
    }

    fun saveAll() {
        val dir = configDir ?: return
        profiles.forEach { profile ->
            val file = File(dir, "${profile.name.replace(" ", "_")}.json")
            file.writeText(gson.toJson(profile))
        }
    }

    fun saveProfile(profile: KeyMapProfile) {
        val dir = configDir ?: return
        val file = File(dir, "${profile.name.replace(" ", "_")}.json")
        file.writeText(gson.toJson(profile))
        Log.d(TAG, "Saved profile: ${profile.name}")
    }

    fun deleteProfile(profile: KeyMapProfile) {
        val dir = configDir ?: return
        val file = File(dir, "${profile.name.replace(" ", "_")}.json")
        file.delete()
        profiles.remove(profile)
        Log.d(TAG, "Deleted profile: ${profile.name}")
    }

    fun addProfile(profile: KeyMapProfile) {
        profiles.add(profile)
        saveProfile(profile)
    }

    fun setActiveProfile(context: Context, profile: KeyMapProfile) {
        activeProfile = profile
        File(context.filesDir, ACTIVE_PROFILE_FILE).writeText(profile.name)
    }

    private fun loadActiveProfile(context: Context) {
        val file = File(context.filesDir, ACTIVE_PROFILE_FILE)
        if (file.exists()) {
            val name = file.readText().trim()
            activeProfile = profiles.find { it.name == name } ?: profiles.firstOrNull()
        } else {
            activeProfile = profiles.firstOrNull()
        }
    }

    fun getMappings(): List<KeyMapEntry> = activeProfile?.mappings ?: emptyList()

    fun addMapping(entry: KeyMapEntry) {
        activeProfile?.mappings?.add(entry)
        activeProfile?.let { saveProfile(it) }
    }

    fun removeMapping(index: Int) {
        activeProfile?.mappings?.let {
            if (index in it.indices) {
                it.removeAt(index)
                activeProfile?.let { p -> saveProfile(p) }
            }
        }
    }

    fun updateMapping(index: Int, entry: KeyMapEntry) {
        activeProfile?.mappings?.let {
            if (index in it.indices) {
                it[index] = entry
                activeProfile?.let { p -> saveProfile(p) }
            }
        }
    }

    fun save() {
        activeProfile?.let { saveProfile(it) }
    }

    private fun getDefaultProfile(): KeyMapProfile {
        return KeyMapProfile(
            name = "Default FPS",
            packageName = "",
            mappings = mutableListOf(
                KeyMapEntry("Fire", android.view.KeyEvent.KEYCODE_SPACE, "SPACE", "tap", 900f, 400f),
                KeyMapEntry("Aim", android.view.KeyEvent.KEYCODE_Q, "Q", "long_press", 540f, 400f, duration = 200),
                KeyMapEntry("Jump", android.view.KeyEvent.KEYCODE_E, "E", "tap", 950f, 700f),
                KeyMapEntry("Reload", android.view.KeyEvent.KEYCODE_R, "R", "tap", 100f, 750f),
                KeyMapEntry("Crouch", android.view.KeyEvent.KEYCODE_C, "C", "tap", 150f, 400f),
                KeyMapEntry("Map", android.view.KeyEvent.KEYCODE_M, "M", "tap", 50f, 50f),
                KeyMapEntry("Weapon 1", android.view.KeyEvent.KEYCODE_1, "1", "tap", 850f, 100f),
                KeyMapEntry("Weapon 2", android.view.KeyEvent.KEYCODE_2, "2", "tap", 850f, 200f),
                KeyMapEntry("Weapon 3", android.view.KeyEvent.KEYCODE_3, "3", "tap", 850f, 300f),
                KeyMapEntry("Move Up", android.view.KeyEvent.KEYCODE_W, "W", "swipe", 200f, 500f, 200f, 400f, 100),
                KeyMapEntry("Move Down", android.view.KeyEvent.KEYCODE_S, "S", "swipe", 200f, 500f, 200f, 600f, 100),
                KeyMapEntry("Move Left", android.view.KeyEvent.KEYCODE_A, "A", "swipe", 200f, 500f, 100f, 500f, 100),
                KeyMapEntry("Move Right", android.view.KeyEvent.KEYCODE_D, "D", "swipe", 200f, 500f, 300f, 500f, 100)
            )
        )
    }
}
