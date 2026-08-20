package com.familyguard.screentime.ui

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppRepository
import com.familyguard.screentime.data.Schedule
import com.familyguard.screentime.util.Constants
import java.util.Calendar
import java.util.Locale

/**
 * Single screen for both creating a new schedule and editing an existing
 * one. Nothing is written to storage until "Save Schedule" is tapped, so
 * backing out mid-edit leaves everything (including any other schedule)
 * untouched.
 */
class ScheduleEditActivity : AppCompatActivity() {

    private lateinit var repository: AppRepository
    private var existingScheduleId: String? = null

    private lateinit var editName: EditText
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var dayCheckboxes: Map<Int, CheckBox>
    private lateinit var btnSelectApps: Button
    private lateinit var textSelectedAppsSummary: TextView
    private lateinit var btnSaveSchedule: Button
    private lateinit var btnDeleteSchedule: Button

    private var startHour = 8
    private var startMinute = 0
    private var endHour = 14
    private var endMinute = 0
    private var restrictedPackages: MutableSet<String> = mutableSetOf()

    private val appSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selected = result.data
                ?.getStringArrayExtra(Constants.EXTRA_RESULT_SELECTED_PACKAGES)
                ?.toSet()
                ?: emptySet()
            restrictedPackages = selected.toMutableSet()
            updateSelectedAppsSummary()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_edit)

        repository = AppRepository(this)
        existingScheduleId = intent.getStringExtra(Constants.EXTRA_SCHEDULE_ID)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        loadExistingScheduleIfAny()
        setupListeners()
        updateTimeButtons()
        updateSelectedAppsSummary()
    }

    private fun bindViews() {
        editName = findViewById(R.id.editScheduleName)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        btnSelectApps = findViewById(R.id.btnSelectApps)
        textSelectedAppsSummary = findViewById(R.id.textSelectedAppsSummary)
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule)
        btnDeleteSchedule = findViewById(R.id.btnDeleteSchedule)

        dayCheckboxes = mapOf(
            Calendar.SUNDAY to findViewById(R.id.checkSun),
            Calendar.MONDAY to findViewById(R.id.checkMon),
            Calendar.TUESDAY to findViewById(R.id.checkTue),
            Calendar.WEDNESDAY to findViewById(R.id.checkWed),
            Calendar.THURSDAY to findViewById(R.id.checkThu),
            Calendar.FRIDAY to findViewById(R.id.checkFri),
            Calendar.SATURDAY to findViewById(R.id.checkSat)
        )
    }

    private fun loadExistingScheduleIfAny() {
        val id = existingScheduleId ?: return
        val schedule = repository.storage.getSchedule(id) ?: run {
            Toast.makeText(this, "Schedule no longer exists", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = "Edit Schedule"
        editName.setText(schedule.name)
        startHour = schedule.startHour
        startMinute = schedule.startMinute
        endHour = schedule.endHour
        endMinute = schedule.endMinute
        restrictedPackages = schedule.restrictedPackages.toMutableSet()
        schedule.repeatDays.forEach { dow -> dayCheckboxes[dow]?.isChecked = true }
        btnDeleteSchedule.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        btnStartTime.setOnClickListener {
            showTimePicker(startHour, startMinute) { h, m ->
                startHour = h; startMinute = m
                updateTimeButtons()
            }
        }
        btnEndTime.setOnClickListener {
            showTimePicker(endHour, endMinute) { h, m ->
                endHour = h; endMinute = m
                updateTimeButtons()
            }
        }
        btnSelectApps.setOnClickListener {
            val intent = Intent(this, AppSelectionActivity::class.java).apply {
                putExtra(Constants.EXTRA_PRESELECTED_PACKAGES, restrictedPackages.toTypedArray())
            }
            appSelectionLauncher.launch(intent)
        }
        btnSaveSchedule.setOnClickListener { saveSchedule() }
        btnDeleteSchedule.setOnClickListener { confirmDelete() }
    }

    private fun showTimePicker(hour: Int, minute: Int, onSet: (Int, Int) -> Unit) {
        // is24HourView = false shows a native AM/PM picker.
        TimePickerDialog(this, { _, h, m -> onSet(h, m) }, hour, minute, false).show()
    }

    private fun updateTimeButtons() {
        btnStartTime.text = formatAmPm(startHour, startMinute)
        btnEndTime.text = formatAmPm(endHour, endMinute)
    }

    private fun formatAmPm(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return String.format(Locale.getDefault(), "%tl:%tM %tp", cal, cal, cal).uppercase()
    }

    private fun updateSelectedAppsSummary() {
        textSelectedAppsSummary.text = if (restrictedPackages.isEmpty()) {
            "No apps selected yet"
        } else {
            "${restrictedPackages.size} app(s) selected"
        }
    }

    private fun saveSchedule() {
        val name = editName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a schedule name", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDays = dayCheckboxes.filterValues { it.isChecked }.keys
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Select at least one repeat day", Toast.LENGTH_SHORT).show()
            return
        }

        if (restrictedPackages.isEmpty()) {
            Toast.makeText(this, "Select at least one app to restrict", Toast.LENGTH_SHORT).show()
            return
        }

        val schedule = Schedule(
            id = existingScheduleId ?: java.util.UUID.randomUUID().toString(),
            name = name,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            repeatDays = selectedDays,
            restrictedPackages = restrictedPackages,
            enabled = true
        )

        if (existingScheduleId != null) {
            repository.storage.updateSchedule(schedule)
        } else {
            repository.storage.addSchedule(schedule)
        }

        Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDelete() {
        val id = existingScheduleId ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete this schedule?")
            .setMessage("Other schedules are not affected.")
            .setPositiveButton("Delete") { _, _ ->
                repository.storage.deleteSchedule(id)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
