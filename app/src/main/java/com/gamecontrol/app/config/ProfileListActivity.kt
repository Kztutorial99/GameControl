package com.gamecontrol.app.config

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gamecontrol.app.R
import com.google.android.material.button.MaterialButton

class ProfileListActivity : AppCompatActivity() {

    private lateinit var rvProfiles: RecyclerView
    private lateinit var adapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_list)

        rvProfiles = findViewById(R.id.rvProfiles)

        adapter = ProfileAdapter(
            onActivate = { profile ->
                ProfileManager.setActiveProfile(this, profile)
                Toast.makeText(this, "Activated: ${profile.name}", Toast.LENGTH_SHORT).show()
                adapter.notifyDataSetChanged()
            },
            onEdit = { profile ->
                ProfileManager.setActiveProfile(this, profile)
                startActivity(Intent(this, KeyMapEditorActivity::class.java))
            },
            onDelete = { profile ->
                if (ProfileManager.profiles.size <= 1) {
                    Toast.makeText(this, "Cannot delete last profile", Toast.LENGTH_SHORT).show()
                    return@ProfileAdapter
                }
                AlertDialog.Builder(this)
                    .setTitle("Delete \"${profile.name}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        ProfileManager.deleteProfile(profile)
                        adapter.notifyDataSetChanged()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rvProfiles.layoutManager = LinearLayoutManager(this)
        rvProfiles.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnNewProfile).setOnClickListener {
            showNewProfileDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        ProfileManager.loadAll()
        adapter.notifyDataSetChanged()
    }

    private fun showNewProfileDialog() {
        val et = EditText(this).apply {
            hint = "Profile name"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("New Profile")
            .setView(et)
            .setPositiveButton("Create") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isNotEmpty()) {
                    ProfileManager.addProfile(KeyMapProfile(name = name))
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class ProfileAdapter(
    private val onActivate: (KeyMapProfile) -> Unit,
    private val onEdit: (KeyMapProfile) -> Unit,
    private val onDelete: (KeyMapProfile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvProfileName)
        val tvMappings: TextView = view.findViewById(R.id.tvMappingCount)
        val tvActive: TextView = view.findViewById(R.id.tvActive)
        val btnActivate: MaterialButton = view.findViewById(R.id.btnActivate)
        val btnEditProfile: MaterialButton = view.findViewById(R.id.btnEditProfile)
        val btnDeleteProfile: ImageButton = view.findViewById(R.id.btnDeleteProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = ProfileManager.profiles[position]
        val isActive = profile == ProfileManager.activeProfile

        holder.tvName.text = profile.name
        holder.tvMappings.text = "${profile.mappings.size} mappings"
        holder.tvActive.visibility = if (isActive) View.VISIBLE else View.GONE

        holder.btnActivate.text = if (isActive) "Active" else "Activate"
        holder.btnActivate.isEnabled = !isActive
        holder.btnActivate.setOnClickListener { onActivate(profile) }
        holder.btnEditProfile.setOnClickListener { onEdit(profile) }
        holder.btnDeleteProfile.setOnClickListener { onDelete(profile) }
    }

    override fun getItemCount() = ProfileManager.profiles.size
}
