package com.ap.timeuntil.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.ap.timeuntil.MainActivity
import com.ap.timeuntil.R
import com.ap.timeuntil.data.EventRepository
import com.ap.timeuntil.utils.TimeCalculator
import java.text.SimpleDateFormat
import java.util.*

class EventWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val PREFS_NAME = "widget_prefs"
        private const val PREF_PREFIX_KEY = "widget_event_"

        // Manual refresh action for widget
        const val ACTION_MANUAL_REFRESH = "com.ap.timeuntil.ACTION_MANUAL_REFRESH"

        // Breakpoints for width and height in dp
        private const val WIDTH_SMALL_BREAKPOINT = 150
        private const val WIDTH_MEDIUM_BREAKPOINT = 220
        private const val HEIGHT_SMALL_BREAKPOINT = 150

        fun saveEventIdForWidget(context: Context, appWidgetId: Int, eventId: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(PREF_PREFIX_KEY + appWidgetId, eventId).apply()
        }

        fun loadEventIdForWidget(context: Context, appWidgetId: Int): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1)
        }

        fun deleteEventIdForWidget(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(PREF_PREFIX_KEY + appWidgetId).apply()
        }

        /**
         * Determines which layout to use based on the widget's width and height.
         */
        private fun getLayoutForSize(width: Int, height: Int): Int {
            // 2x2 widget
            val isSmall = width < WIDTH_SMALL_BREAKPOINT && height < HEIGHT_SMALL_BREAKPOINT
            // 3x2 widget
            val isMedium = width < WIDTH_MEDIUM_BREAKPOINT && height < HEIGHT_SMALL_BREAKPOINT

            return when {
                isSmall -> R.layout.widget_event_small // 2x2
                isMedium -> R.layout.widget_event // 3x2
                else -> R.layout.widget_event_large // All other sizes
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_MANUAL_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // 1. Show Loading Animation (Spinner) immediately
                showLoadingState(context, appWidgetManager, appWidgetId)

                // 2. Perform Update (Data fetch + Reset to Icon)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            } else {
                // Fallback: refresh all widgets
                val component = ComponentName(context, EventWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                for (id in ids) {
                    showLoadingState(context, appWidgetManager, id)
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    // New Helper: Immediately flips the ViewFlipper to the progress bar (Child 1)
    private fun showLoadingState(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val layoutId = getLayoutForSize(width, height)

        val views = RemoteViews(context.packageName, layoutId)
        // Show Child 1 (ProgressBar)
        views.setDisplayedChild(R.id.viewFlipperWidget, 1)

        // Push immediate update
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deleteEventIdForWidget(context, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val eventId = loadEventIdForWidget(context, appWidgetId)

        // Get the widget options to determine its size
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        // Select the correct layout
        val layoutId = getLayoutForSize(width, height)
        val views = RemoteViews(context.packageName, layoutId)

        // IMPORTANT: Always reset to Child 0 (Static Icon) when data is ready
        views.setDisplayedChild(R.id.viewFlipperWidget, 0)

        if (eventId != -1L) {
            val eventRepository = EventRepository(context)
            val event = eventRepository.getEventById(eventId)

            if (event != null) {
                views.setTextViewText(R.id.textViewWidgetEventName, event.name)

                val timeRemaining = TimeCalculator.calculateTimeRemaining(event.dateTimeMillis)
                val timeText =
                    TimeCalculator.formatTimeRemaining(timeRemaining, includeSeconds = false)
                views.setTextViewText(R.id.textViewWidgetTimeRemaining, timeText)

                val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                val dateTimeText = dateFormat.format(Date(event.dateTimeMillis))
                views.setTextViewText(R.id.textViewWidgetDateTime, dateTimeText)

                // 🔴 Change text color depending on dark / light mode
                val isDarkMode = (context.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val normalColorRes = if (isDarkMode) {
                    android.R.color.white
                } else {
                    android.R.color.black
                }

                val colorRes = if (timeRemaining.isPast) {
                    android.R.color.holo_red_light
                } else {
                    normalColorRes
                }

                val textColor = ContextCompat.getColor(context, colorRes)
                views.setTextColor(R.id.textViewWidgetEventName, textColor)
                views.setTextColor(R.id.textViewWidgetTimeRemaining, textColor)
                views.setTextColor(R.id.textViewWidgetDateTime, textColor)

            } else {
                views.setTextViewText(R.id.textViewWidgetEventName, "No event selected")
                views.setTextViewText(R.id.textViewWidgetTimeRemaining, "")
                views.setTextViewText(R.id.textViewWidgetDateTime, "")
            }

            // Open app when tapping event name
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.textViewWidgetEventName, pendingIntent)

            // 🔄 Manual refresh button click logic
            val refreshIntent = Intent(context, EventWidgetProvider::class.java).apply {
                action = ACTION_MANUAL_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Attach listener to the specific ImageButton inside the Flipper
            views.setOnClickPendingIntent(R.id.imageButtonWidgetRefresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}