package com.ap.timeuntil.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Event(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val notes: String = "",
    val dateTimeMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),

    // 🔔 NEW: per-event notification settings
    val notificationEnabled: Boolean = false,
    val notificationMinutesBefore: Int = 0
) : Parcelable
