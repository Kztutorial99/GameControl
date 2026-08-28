package com.gamecontrol.app.config

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gamecontrol.app.R
import com.google.android.material.button.MaterialButton

class KeyMapEditorActivity : AppCompatActivity() {

    companion object {
        const val CALIBRATE_REQUEST = 1001
        var pendingCalibrationIndex = -1
    }

    private lateinit var rvMappings: RecyclerView
    private lateinit var tvProfileName: TextView
    private lateinit var adapter: MappingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keymap_editor)

        rvMappings = findViewById(R.id.rvMappings)
        tvProfileName = findViewById(R.id.tvProfileName)

        val profile = ProfileManager.activeProfile
        tvProfileName.text = "Profile: ${profile?.name ?: "Default"}"

        adapter = MappingAdapter(
            mappings = profile?.mappings ?: mutableListOf(),
            onEdit = { index, entry -> showEditDialog(index, entry) },
            onDelete = { index ->
                AlertDialog.Builder(this)
                    .setTitle("Delete mapping?")
                    .setMessage("Remove \"${profile?.mappings?.get(index)?.name}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        ProfileManager.removeMapping(index)
                        adapter.notifyItemRemoved(index)
                        adapter.notifyDataSetChanged()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rvMappings.layoutManager = LinearLayoutManager(this)
        rvMappings.adapter = adapter

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<android.widget.ImageButton>(R.id.btnAdd).setOnClickListener {
            showEditDialog(-1, KeyMapEntry())
        }

        findViewById<MaterialButton>(R.id.btnCalibrate).setOnClickListener {
            val intent = Intent(this, CalibrationActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            ProfileManager.save()
            Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(index: Int, entry: KeyMapEntry) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_mapping, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val tvKeyCapture = view.findViewById<TextView>(R.id.tvKeyCapture)
        val spAction = view.findViewById<Spinner>(R.id.spAction)
        val etX = view.findViewById<EditText>(R.id.etX)
        val etY = view.findViewById<EditText>(R.id.etY)
        val etX2 = view.findViewById<EditText>(R.id.etX2)
        val etY2 = view.findViewById<EditText>(R.id.etY2)
        val etDuration = view.findViewById<EditText>(R.id.etDuration)

        etName.setText(entry.name)
        tvKeyCapture.text = if (entry.keyLabel.isNotEmpty()) entry.keyLabel else "Press a key..."
        etX.setText(entry.x.toInt().toString())
        etY.setText(entry.y.toInt().toString())
        etX2.setText(entry.x2.toInt().toString())
        etY2.setText(entry.y2.toInt().toString())
        etDuration.setText(entry.duration.toString())

        val actions = arrayOf("Tap", "Swipe", "Long Press", "Macro")
        spAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions)
        spAction.setSelection(when (entry.action) {
            "swipe" -> 1
            "long_press" -> 2
            "macro" -> 3
            else -> 0
        })

        var capturedKeyCode = entry.keyCode
        var capturedLabel = entry.keyLabel

        tvKeyCapture.setOnClickListener {
            tvKeyCapture.text = "Waiting for key..."
            tvKeyCapture.isFocusableInTouchMode = true
            tvKeyCapture.requestFocus()
        }

        tvKeyCapture.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                capturedKeyCode = keyCode
                capturedLabel = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "")
                tvKeyCapture.text = capturedLabel
                true
            } else false
        }

        val btnPickCoord = view.findViewById<MaterialButton>(R.id.btnPickCoordinate)
        btnPickCoord.setOnClickListener {
            pendingCalibrationIndex = index
            val intent = Intent(this, CalibrationActivity::class.java)
            intent.putExtra("mode", "pick")
            startActivityForResult(intent, CALIBRATE_REQUEST)
            return@setOnClickListener
        }

        val actionMap = mapOf(0 to "tap", 1 to "swipe", 2 to "long_press", 3 to "macro")

        AlertDialog.Builder(this)
            .setTitle(if (index >= 0) "Edit Mapping" else "New Mapping")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val newEntry = KeyMapEntry(
                    name = etName.text.toString().ifEmpty { "Unnamed" },
                    keyCode = capturedKeyCode,
                    keyLabel = capturedLabel,
                    action = actionMap[spAction.selectedItemPosition] ?: "tap",
                    x = etX.text.toString().toFloatOrNull() ?: 0f,
                    y = etY.text.toString().toFloatOrNull() ?: 0f,
                    x2 = etX2.text.toString().toFloatOrNull() ?: 0f,
                    y2 = etY2.text.toString().toFloatOrNull() ?: 0f,
                    duration = etDuration.text.toString().toLongOrNull() ?: 50
                )

                if (index >= 0) {
                    ProfileManager.updateMapping(index, newEntry)
                    adapter.notifyItemChanged(index)
                } else {
                    ProfileManager.addMapping(newEntry)
                    adapter.notifyItemInserted(adapter.itemCount - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CALIBRATE_REQUEST && resultCode == Activity.RESULT_OK) {
            val x = data?.getFloatExtra("x", 0f) ?: 0f
            val y = data?.getFloatExtra("y", 0f) ?: 0f
            // Update the last edited entry's coordinates
            val idx = pendingCalibrationIndex
            if (idx >= 0) {
                val profile = ProfileManager.activeProfile ?: return
                if (idx in profile.mappings.indices) {
                    profile.mappings[idx].x = x
                    profile.mappings[idx].y = y
                    adapter.notifyItemChanged(idx)
                }
            }
        }
    }
}
