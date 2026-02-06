package com.ap.timeuntil

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ap.timeuntil.adapter.EventAdapter
import com.ap.timeuntil.data.Event
import com.ap.timeuntil.data.EventRepository
import com.ap.timeuntil.databinding.ActivityMainBinding
import com.ap.timeuntil.dialog.AddEditEventDialog
import com.ap.timeuntil.widget.WidgetUpdateWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var eventRepository: EventRepository

    // Adapter for the main list
    private lateinit var eventAdapter: EventAdapter
    // Adapter for the search results list
    private lateinit var searchAdapter: EventAdapter

    private var allEvents = mutableListOf<Event>()
    private var filteredEvents = mutableListOf<Event>() // For main list
    private var searchResults = mutableListOf<Event>()  // For search view

    companion object {
        private const val TAG = "MainActivity"
    }

    /** ---- Android 12+ exact alarm request handling ---- */
    @RequiresApi(Build.VERSION_CODES.S)
    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkExactAlarmPermission()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkExactAlarmPermission() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(
                this,
                "Please enable Exact Alarms so reminders can work.",
                Toast.LENGTH_LONG
            ).show()

            exactAlarmPermissionLauncher.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            )
        }
    }

    // ----------------------- OnCreate -----------------------
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOTE: We removed setSupportActionBar(binding.toolbar) because
        // the SearchBar handles the menu now.

        // Initialize functionality
        checkExactAlarmPermission()
        eventRepository = EventRepository(this)
        WidgetUpdateWorker.scheduleWork(this)

        // Setup UI components
        setupDeveloperLinks() // Expandable logic here
        setupRecyclerViews()
        setupSearchAndMenu()  // New Search & Menu Logic
        loadEvents()

        binding.fabAddEvent.setOnClickListener {
            Log.d(TAG, "Add Event button clicked")
            showAddEditEventDialog()
        }
    }

    // ----------------------- Developer Info Logic -----------------------
    private fun setupDeveloperLinks() {
        // 1. EXPAND / COLLAPSE ANIMATION
        binding.headerDevInfo.setOnClickListener {
            // Check current state
            val isVisible = binding.layoutDevDetails.visibility == View.VISIBLE

            // Start smooth transition on the root layout
            TransitionManager.beginDelayedTransition(binding.rootLayout as ViewGroup, AutoTransition())

            if (isVisible) {
                // Collapse
                binding.layoutDevDetails.visibility = View.GONE
                binding.imgArrow.animate().rotation(0f).start() // Rotate arrow back
            } else {
                // Expand
                binding.layoutDevDetails.visibility = View.VISIBLE
                binding.imgArrow.animate().rotation(180f).start() // Rotate arrow up
            }
        }

        // 2. CLICKABLE LINKS INSIDE

        // Email Link
        binding.tvEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:ap0803apap@gmail.com")
            }
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        // GitHub Link
        binding.tvGithub.setOnClickListener {
            val githubUrl = "https://github.com/ap0803apap-sketch/Time-Until-by-AP"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
            try {
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
            }
        }

        // Admin Instructions Dialog
        binding.tvAdminInfo.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Prevent Data Loss")
                .setIcon(R.drawable.ic_shield)
                .setMessage(
                    "To prevent accidental uninstallation and losing your events:\n\n" +
                            "1. Go to your Phone Settings.\n" +
                            "2. Search for 'Device Admin Apps'.\n" +
                            "3. Find this app in the list.\n" +
                            "4. Toggle the switch to ON.\n\n" +
                            "This will block the app from being uninstalled until you turn it off."
                )
                .setPositiveButton("Open Settings") { _, _ ->
                    try {
                        // 1. Try opening the specific DeviceAdminSettings component directly
                        val intent = Intent()
                        intent.setClassName("com.android.settings", "com.android.settings.DeviceAdminSettings")
                        startActivity(intent)
                    } catch (e: Exception) {
                        // 2. Fallback: Try generic Security Settings
                        try {
                            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                        } catch (e2: Exception) {
                            // 3. Last Resort: Open Main Settings
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    // ----------------------- RecyclerView Setup -----------------------
    private fun setupRecyclerViews() {
        // Common actions for both lists
        val onEdit = { event: Event -> showAddEditEventDialog(event) }
        val onDelete = { event: Event -> showDeleteConfirmation(event) }
        val onDuplicate = { event: Event ->
            eventRepository.duplicateEvent(event)
            loadEvents()
            binding.searchView.hide() // Close search if action taken
        }

        // 1. Main List Adapter
        eventAdapter = EventAdapter(
            events = filteredEvents,
            onEditClick = onEdit,
            onDeleteClick = onDelete,
            onDuplicateClick = onDuplicate
        )
        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = eventAdapter
        }

        // 2. Search Results Adapter
        searchAdapter = EventAdapter(
            events = searchResults,
            onEditClick = onEdit,
            onDeleteClick = onDelete,
            onDuplicateClick = onDuplicate
        )
        binding.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchAdapter
        }
    }

    // ----------------------- Search & Menu Logic -----------------------
    private fun setupSearchAndMenu() {
        // A. Handle Menu Clicks (Sorting) on the SearchBar
        binding.searchBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort_date_created_old -> {
                    sortEventsBy { it.createdAtMillis }; true
                }
                R.id.sort_date_created_new -> {
                    sortEventsByDescending { it.createdAtMillis }; true
                }
                R.id.sort_event_date_old -> {
                    sortEventsBy { it.dateTimeMillis }; true
                }
                R.id.sort_event_date_new -> {
                    sortEventsByDescending { it.dateTimeMillis }; true
                }
                R.id.sort_name_a_z -> {
                    sortEventsBy { it.name.lowercase() }; true
                }
                R.id.sort_name_z_a -> {
                    sortEventsByDescending { it.name.lowercase() }; true
                }
                else -> false
            }
        }

        // B. Handle Search Query Input
        binding.searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSearchEvents(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Optional: Populate search list when view opens
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWN) {
                filterSearchEvents(binding.searchView.text.toString())
            }
        }
    }

    // ----------------------- Sorting Helpers -----------------------
    private fun <T : Comparable<T>> sortEventsBy(selector: (Event) -> T) {
        allEvents.sortBy(selector)
        // Update Main List
        filteredEvents.clear()
        filteredEvents.addAll(allEvents)
        eventAdapter.notifyDataSetChanged()

        // Update Search List if active (optional, but good UX)
        if (binding.searchView.isShowing) {
            filterSearchEvents(binding.searchView.text.toString())
        }

        updateEmptyView()
    }

    private fun <T : Comparable<T>> sortEventsByDescending(selector: (Event) -> T) {
        allEvents.sortByDescending(selector)
        // Update Main List
        filteredEvents.clear()
        filteredEvents.addAll(allEvents)
        eventAdapter.notifyDataSetChanged()

        // Update Search List if active
        if (binding.searchView.isShowing) {
            filterSearchEvents(binding.searchView.text.toString())
        }

        updateEmptyView()
    }

    // ----------------------- Load & Filter Events -----------------------
    private fun loadEvents() {
        allEvents = eventRepository.getEvents()

        filteredEvents.clear()
        filteredEvents.addAll(allEvents)

        eventAdapter.notifyDataSetChanged()
        updateEmptyView()
    }

    private fun filterSearchEvents(query: String) {
        searchResults.clear()
        if (query.isEmpty()) {
            // Show all if query empty (or show recent, logic can vary)
            searchResults.addAll(allEvents)
        } else {
            val lowerQuery = query.lowercase()
            searchResults.addAll(allEvents.filter {
                it.name.lowercase().contains(lowerQuery)
            })
        }
        searchAdapter.notifyDataSetChanged()
    }

    private fun updateEmptyView() {
        if (allEvents.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.recyclerViewEvents.visibility = View.GONE
        } else {
            binding.textViewEmpty.visibility = View.GONE
            binding.recyclerViewEvents.visibility = View.VISIBLE
        }
    }

    // ----------------------- Dialogs -----------------------
    private fun showAddEditEventDialog(event: Event? = null) {
        AddEditEventDialog(
            context = this,
            event = event,
            onSave = { savedEvent ->
                if (event == null) eventRepository.addEvent(savedEvent)
                else eventRepository.updateEvent(savedEvent)

                loadEvents()
            }
        ).show()
    }

    private fun showDeleteConfirmation(event: Event) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                eventRepository.deleteEvent(event.id)
                loadEvents()
                // If we were searching, refresh that view too
                if (binding.searchView.isShowing) {
                    binding.searchView.setText("") // Reset or re-filter
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}