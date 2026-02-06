package com.ap.timeuntil.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ap.timeuntil.R
import com.ap.timeuntil.data.Event
import com.ap.timeuntil.data.EventRepository
import com.ap.timeuntil.notification.NotificationScheduler
import com.ap.timeuntil.utils.TimeCalculator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private var events: MutableList<Event>,
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit,
    private val onDuplicateClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            notifyDataSetChanged()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        startUpdating()
    }

    private fun startUpdating() {
        handler.post(updateRunnable)
    }

    fun stopUpdating() {
        handler.removeCallbacks(updateRunnable)
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewEventName: TextView = itemView.findViewById(R.id.textViewEventName)
        val textViewDateTime: TextView = itemView.findViewById(R.id.textViewDateTime)
        val textViewTimeRemaining: TextView = itemView.findViewById(R.id.textViewTimeRemaining)
        val imageButtonEdit: ImageButton = itemView.findViewById(R.id.imageButtonEdit)
        val imageButtonDelete: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        val imageButtonDuplicate: ImageButton = itemView.findViewById(R.id.imageButtonDuplicate)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        var event = events[position]

        holder.textViewEventName.text = event.name

        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        holder.textViewDateTime.text = dateFormat.format(Date(event.dateTimeMillis))

        val timeRemaining = TimeCalculator.calculateTimeRemaining(event.dateTimeMillis)
        holder.textViewTimeRemaining.text =
            TimeCalculator.formatTimeRemaining(timeRemaining, true)

        // Edit / delete / duplicate
        holder.imageButtonEdit.setOnClickListener { onEditClick(event) }
        holder.imageButtonDelete.setOnClickListener { onDeleteClick(event) }
        holder.imageButtonDuplicate.setOnClickListener { onDuplicateClick(event) }

    }

    // Dummy listener reference helper to avoid crash if reused (never actually called)
    private fun onCheckedChangeDummy(button: android.widget.CompoundButton, checked: Boolean) {
        // No-op
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    fun filterEvents(query: String) {
        notifyDataSetChanged()
    }
}
