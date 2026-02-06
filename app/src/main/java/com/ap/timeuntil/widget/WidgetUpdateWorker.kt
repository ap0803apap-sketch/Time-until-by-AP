package com.ap.timeuntil.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.*
import com.ap.timeuntil.R
import com.ap.timeuntil.data.EventRepository
import com.ap.timeuntil.utils.TimeCalculator
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        updateAllWidgets()
        return Result.success()
    }

    private fun updateAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetComponent = ComponentName(applicationContext, EventWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

        for (appWidgetId in appWidgetIds) {
            val eventId = EventWidgetProvider.loadEventIdForWidget(applicationContext, appWidgetId)
            if (eventId != -1L) {

                // Get the widget options to determine its size
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                // Select the correct layout
                val layoutId = getLayoutForSize(width, height)
                val views = RemoteViews(
                    applicationContext.packageName,
                    layoutId
                )

                // IMPORTANT: Ensure we display the Static Icon (Child 0) for background updates
                views.setDisplayedChild(R.id.viewFlipperWidget, 0)

                val eventRepository = EventRepository(applicationContext)
                val event = eventRepository.getEventById(eventId)

                if (event != null) {
                    // Update all fields
                    views.setTextViewText(R.id.textViewWidgetEventName, event.name)

                    val timeRemaining = TimeCalculator.calculateTimeRemaining(event.dateTimeMillis)
                    val timeText = TimeCalculator.formatTimeRemaining(timeRemaining, includeSeconds = false)
                    views.setTextViewText(R.id.textViewWidgetTimeRemaining, timeText)

                    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                    val dateTimeText = dateFormat.format(Date(event.dateTimeMillis))
                    views.setTextViewText(R.id.textViewWidgetDateTime, dateTimeText)

                    // 🔴 Change text color depending on dark / light mode
                    val isDarkMode = (applicationContext.resources.configuration.uiMode and
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

                    val textColor = ContextCompat.getColor(applicationContext, colorRes)
                    views.setTextColor(R.id.textViewWidgetEventName, textColor)
                    views.setTextColor(R.id.textViewWidgetTimeRemaining, textColor)
                    views.setTextColor(R.id.textViewWidgetDateTime, textColor)

                    // 🔄 Manual refresh button also on auto updates
                    val refreshIntent = Intent(applicationContext, EventWidgetProvider::class.java).apply {
                        action = EventWidgetProvider.ACTION_MANUAL_REFRESH
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }

                    val refreshPendingIntent = PendingIntent.getBroadcast(
                        applicationContext,
                        appWidgetId,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    views.setOnClickPendingIntent(R.id.imageButtonWidgetRefresh, refreshPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    companion object {
        private const val WORK_NAME = "widget_update_work"

        // Breakpoints for width and height in dp
        private const val WIDTH_SMALL_BREAKPOINT = 150
        private const val WIDTH_MEDIUM_BREAKPOINT = 220
        private const val HEIGHT_SMALL_BREAKPOINT = 150

        private fun getLayoutForSize(width: Int, height: Int): Int {
            val isSmall = width < WIDTH_SMALL_BREAKPOINT && height < HEIGHT_SMALL_BREAKPOINT
            val isMedium = width < WIDTH_MEDIUM_BREAKPOINT && height < HEIGHT_SMALL_BREAKPOINT

            return when {
                isSmall -> R.layout.widget_event_small
                isMedium -> R.layout.widget_event
                else -> R.layout.widget_event_large
            }
        }

        fun scheduleWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}