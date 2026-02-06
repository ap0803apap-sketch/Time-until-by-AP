package com.ap.timeuntil.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EventRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_EVENTS = "events_list"
    }

    fun saveEvents(events: List<Event>) {
        val json = gson.toJson(events)
        sharedPreferences.edit().putString(KEY_EVENTS, json).apply()
    }

    fun getEvents(): MutableList<Event> {
        val json = sharedPreferences.getString(KEY_EVENTS, null)
        return if (json != null && json.isNotEmpty()) {
            try {
                val type = object : TypeToken<MutableList<Event>>() {}.type
                gson.fromJson<MutableList<Event>>(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                e.printStackTrace()
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    fun addEvent(event: Event) {
        val events = getEvents().toMutableList()
        events.add(event)
        saveEvents(events)
    }

    fun updateEvent(updatedEvent: Event) {
        val events = getEvents().toMutableList()
        val index = events.indexOfFirst { it.id == updatedEvent.id }
        if (index != -1) {
            events[index] = updatedEvent
            saveEvents(events)
        }
    }

    fun deleteEvent(eventId: Long) {
        val events = getEvents().toMutableList()
        events.removeAll { it.id == eventId }
        saveEvents(events)
    }

    fun duplicateEvent(event: Event) {
        val duplicatedEvent = event.copy(
            id = System.currentTimeMillis(),
            name = event.name + " (Copy)",
            createdAtMillis = System.currentTimeMillis()
        )
        addEvent(duplicatedEvent)
    }

    fun getEventById(eventId: Long): Event? {
        return getEvents().find { it.id == eventId }
    }
}
