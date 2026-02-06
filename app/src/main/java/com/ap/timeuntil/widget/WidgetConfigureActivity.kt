package com.ap.timeuntil.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ap.timeuntil.adapter.EventAdapter
import com.ap.timeuntil.data.Event
import com.ap.timeuntil.data.EventRepository
import com.ap.timeuntil.databinding.ActivityWidgetConfigureBinding

class WidgetConfigureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigureBinding
    private lateinit var eventRepository: EventRepository
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var eventAdapter: EventAdapter
    private var events = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        binding = ActivityWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        eventRepository = EventRepository(this)

        setupRecyclerView()
        loadEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(
            events = events,
            onEditClick = { },
            onDeleteClick = { },
            onDuplicateClick = { }
        )

        binding.recyclerViewSelectEvent.apply {
            layoutManager = LinearLayoutManager(this@WidgetConfigureActivity)
            adapter = eventAdapter

            addOnItemTouchListener(
                RecyclerItemClickListener(
                    context,
                    this,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int) {
                            val selectedEvent = events[position]
                            configureWidget(selectedEvent)
                        }

                        override fun onLongItemClick(view: View, position: Int) {}
                    }
                )
            )
        }
    }

    private fun loadEvents() {
        events = eventRepository.getEvents()
        eventAdapter.updateEvents(events)

        if (events.isEmpty()) {
            binding.textViewNoEvents.visibility = View.VISIBLE
            binding.recyclerViewSelectEvent.visibility = View.GONE
        } else {
            binding.textViewNoEvents.visibility = View.GONE
            binding.recyclerViewSelectEvent.visibility = View.VISIBLE
        }
    }

    private fun configureWidget(event: Event) {
        EventWidgetProvider.saveEventIdForWidget(this, appWidgetId, event.id)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val views = android.widget.RemoteViews(packageName, com.ap.timeuntil.R.layout.widget_event)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)

        val updateIntent = Intent(this, EventWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        sendBroadcast(updateIntent)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        eventAdapter.stopUpdating()
    }
}