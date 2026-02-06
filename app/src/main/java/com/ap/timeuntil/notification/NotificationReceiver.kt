package com.ap.timeuntil.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ap.timeuntil.MainActivity
import com.ap.timeuntil.R
import com.ap.timeuntil.data.EventRepository
import java.text.SimpleDateFormat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra("event_id", -1L)
        if (eventId == -1L) return

        val repository = EventRepository(context)
        val event = repository.getEventById(eventId) ?: return

        val dateFormat = SimpleDateFormat(
            "MMM dd, yyyy 'at' hh:mm a",
            Locale.getDefault()
        )
        val whenText = dateFormat.format(Date(event.dateTimeMillis))

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val requestCode = (event.id and 0x7FFFFFFF).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            context,
            NotificationScheduler.getChannelId()
        )
            .setSmallIcon(R.mipmap.ic_launcher) // app icon
            .setContentTitle(event.name)
            .setContentText("Event at $whenText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Event at $whenText"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.notify(requestCode, builder.build())
    }
}
