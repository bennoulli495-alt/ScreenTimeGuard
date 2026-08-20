package com.familyguard.screentime.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppRepository
import com.familyguard.screentime.data.Schedule
import com.familyguard.screentime.util.Constants

/**
 * Lists all independently-managed schedules with per-schedule Edit/Delete/
 * Enable controls. There is deliberately no "reset all" action — each
 * schedule is created, changed, and removed on its own.
 */
class ScheduleListActivity : AppCompatActivity() {

    private lateinit var repository: AppRepository
    private lateinit var recyclerSchedules: RecyclerView
    private lateinit var btnAddSchedule: Button
    private lateinit var textEmptyState: TextView
    private lateinit var adapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_list)

        repository = AppRepository(this)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerSchedules = findViewById(R.id.recyclerSchedules)
        btnAddSchedule = findViewById(R.id.btnAddSchedule)
        textEmptyState = findViewById(R.id.textEmptyState)

        recyclerSchedules.layoutManager = LinearLayoutManager(this)
        adapter = ScheduleAdapter(
            schedules = emptyList(),
            onEdit = { schedule ->
                val intent = Intent(this, ScheduleEditActivity::class.java).apply {
                    putExtra(Constants.EXTRA_SCHEDULE_ID, schedule.id)
                }
                startActivity(intent)
            },
            onDelete = { schedule -> confirmDelete(schedule) },
            onToggleEnabled = { schedule, enabled ->
                repository.storage.setScheduleEnabled(schedule.id, enabled)
            }
        )
        recyclerSchedules.adapter = adapter

        btnAddSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val schedules = repository.storage.getSchedules()
        adapter.updateData(schedules)
        textEmptyState.visibility = if (schedules.isEmpty()) View.VISIBLE else View.GONE
        recyclerSchedules.visibility = if (schedules.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(schedule: Schedule) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${schedule.name}\"?")
            .setMessage("This only removes this schedule. Other schedules are not affected.")
            .setPositiveButton("Delete") { _, _ ->
                repository.storage.deleteSchedule(schedule.id)
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
