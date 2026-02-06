package com.ap.timeuntil.dialog

// Import the new Material 3 pickers
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity // Needed to get FragmentManager
import com.ap.timeuntil.R
import com.ap.timeuntil.data.Event
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker // New Date Picker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker // New Time Picker
import com.google.android.material.timepicker.TimeFormat // New TimeFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone // Needed for UTC conversion

class AddEditEventDialog(
    private val context: Context,
    private val event: Event? = null,
    private val onSave: (Event) -> Unit
) {

    private var selectedDateTimeMillis: Long? = event?.dateTimeMillis
    private lateinit var buttonSelectDate: MaterialButton
    private lateinit var buttonSelectTime: MaterialButton

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_event, null)

        val editTextEventName: TextInputEditText = dialogView.findViewById(R.id.editTextEventName)
        val editTextNotes: TextInputEditText = dialogView.findViewById(R.id.editTextNotes)
        buttonSelectDate = dialogView.findViewById(R.id.buttonSelectDate)
        buttonSelectTime = dialogView.findViewById(R.id.buttonSelectTime)
        val buttonSave: MaterialButton = dialogView.findViewById(R.id.buttonSave)
        val buttonCancel: MaterialButton = dialogView.findViewById(R.id.buttonCancel)

        event?.let {
            editTextEventName.setText(it.name)
            editTextNotes.setText(it.notes)
            updateDateTimeButtons()
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (event == null) R.string.add_event else R.string.edit_event)
            .setView(dialogView)
            .create()

        buttonSelectDate.setOnClickListener {
            showDatePicker()
        }

        buttonSelectTime.setOnClickListener {
            showTimePicker()
        }

        buttonSave.setOnClickListener {
            val name = editTextEventName.text.toString().trim()
            val notes = editTextNotes.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(context, R.string.event_name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDateTimeMillis == null) {
                Toast.makeText(context, R.string.date_time_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedEvent = if (event != null) {
                event.copy(
                    name = name,
                    notes = notes,
                    dateTimeMillis = selectedDateTimeMillis!!
                )
            } else {
                Event(
                    name = name,
                    notes = notes,
                    dateTimeMillis = selectedDateTimeMillis!!
                )
            }

            onSave(savedEvent)
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * REPLACED with MaterialDatePicker
     */
    private fun showDatePicker() {
        // Get the FragmentManager from the activity context
        val fragmentManager = (context as? AppCompatActivity)?.supportFragmentManager
        if (fragmentManager == null) {
            Log.e("AddEditEventDialog", "Cannot get SupportFragmentManager from context")
            return
        }

        // MaterialDatePicker works in UTC. Use today's date in UTC as a default
        // if no date is selected, otherwise use the existing selection.
        val selection = selectedDateTimeMillis ?: MaterialDatePicker.todayInUtcMilliseconds()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(context.getString(R.string.select_date))
            .setSelection(selection)
            .build()

        datePicker.addOnPositiveButtonClickListener { newSelection ->
            // newSelection is midnight UTC for the selected day.
            // We need to combine this new date with the existing time (if any)
            // in the *local* time zone.

            val newDateUtcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            newDateUtcCalendar.timeInMillis = newSelection

            val finalCalendar = Calendar.getInstance() // Local time zone
            if (selectedDateTimeMillis != null) {
                // Preserve existing time
                finalCalendar.timeInMillis = selectedDateTimeMillis!!
            }

            // Set the new date components from the UTC calendar
            finalCalendar.set(
                newDateUtcCalendar.get(Calendar.YEAR),
                newDateUtcCalendar.get(Calendar.MONTH),
                newDateUtcCalendar.get(Calendar.DAY_OF_MONTH)
            )

            selectedDateTimeMillis = finalCalendar.timeInMillis
            updateDateTimeButtons()
        }

        datePicker.show(fragmentManager, "DatePicker")
    }

    /**
     * REPLACED with MaterialTimePicker
     */
    private fun showTimePicker() {
        val fragmentManager = (context as? AppCompatActivity)?.supportFragmentManager
        if (fragmentManager == null) {
            Log.e("AddEditEventDialog", "Cannot get SupportFragmentManager from context")
            return
        }

        val calendar = Calendar.getInstance()
        if (selectedDateTimeMillis != null) {
            calendar.timeInMillis = selectedDateTimeMillis!!
        }

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePicker = MaterialTimePicker.Builder()
            // Use 12-hour format to match your original "false" setting
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText(context.getString(R.string.select_time))
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedCalendar = Calendar.getInstance()
            if (selectedDateTimeMillis != null) {
                // Preserve existing date
                selectedCalendar.timeInMillis = selectedDateTimeMillis!!
            }

            selectedCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedCalendar.set(Calendar.MINUTE, timePicker.minute)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)

            selectedDateTimeMillis = selectedCalendar.timeInMillis
            updateDateTimeButtons()
        }

        timePicker.show(fragmentManager, "TimePicker")
    }

    private fun updateDateTimeButtons() {
        if (selectedDateTimeMillis != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = Date(selectedDateTimeMillis!!)

            buttonSelectDate.text = dateFormat.format(date)
            buttonSelectTime.text = timeFormat.format(date)
        }
    }
}