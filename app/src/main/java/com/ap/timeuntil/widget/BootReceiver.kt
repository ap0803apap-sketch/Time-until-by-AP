package com.ap.timeuntil.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ap.timeuntil.data.EventRepository
import com.ap.timeuntil.notification.NotificationScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Restart widget updates after device reboot
            WidgetUpdateWorker.scheduleWork(context)

            // 🔔 Reschedule all enabled event notifications
            val repository = EventRepository(context)
            val events = repository.getEvents()
            events.forEach { event ->
                if (event.notificationEnabled && event.notificationMinutesBefore > 0) {
                    NotificationScheduler.scheduleNotification(context, event)
                }
            }
        }
    }
}
