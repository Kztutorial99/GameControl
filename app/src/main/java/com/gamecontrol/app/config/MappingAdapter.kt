package com.gamecontrol.app.config

import android.app.AlertDialog
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gamecontrol.app.R

class MappingAdapter(
    private val mappings: MutableList<KeyMapEntry>,
    private val onEdit: (Int, KeyMapEntry) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<MappingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKeyLabel: TextView = view.findViewById(R.id.tvKeyLabel)
        val tvMappingName: TextView = view.findViewById(R.id.tvMappingName)
        val tvActionType: TextView = view.findViewById(R.id.tvActionType)
        val tvCoordinates: TextView = view.findViewById(R.id.tvCoordinates)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mapping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = mappings[position]
        holder.tvKeyLabel.text = entry.keyLabel.ifEmpty { "?" }
        holder.tvMappingName.text = entry.name.ifEmpty { "Unnamed" }
        holder.tvActionType.text = entry.getActionLabel()
        holder.tvCoordinates.text = entry.getCoordinateText()

        holder.btnEdit.setOnClickListener { onEdit(position, entry) }
        holder.btnDelete.setOnClickListener { onDelete(position) }
    }

    override fun getItemCount() = mappings.size
}
