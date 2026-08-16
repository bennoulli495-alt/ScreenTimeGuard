package com.familyguard.screentime.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppInfo
import com.familyguard.screentime.data.AppRepository

/**
 * Standalone screen for choosing which apps are subject to the restricted
 * window. Kept separate from SettingsActivity so the main settings screen
 * stays short; selections are written straight to shared storage on save.
 */
class AppSelectionActivity : AppCompatActivity() {

    private lateinit var repository: AppRepository
    private lateinit var recyclerApps: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var btnSaveSelection: Button

    private var allApps: List<AppInfo> = emptyList()
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        repository = AppRepository(this)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerApps = findViewById(R.id.recyclerApps)
        editSearch = findViewById(R.id.editSearch)
        btnSaveSelection = findViewById(R.id.btnSaveSelection)

        recyclerApps.layoutManager = LinearLayoutManager(this)

        allApps = repository.getInstallableApps()
        adapter = AppListAdapter(allApps.toMutableList())
        recyclerApps.adapter = adapter

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSaveSelection.setOnClickListener {
            // Merge newly-checked/unchecked state from the adapter's current
            // (possibly filtered) list back into the full app list before saving,
            // so selections made before a search filter are not lost.
            val currentSelections = adapter.selectedPackages()
            val previousSelections = repository.storage.lockedPackages
            val visiblePackages = allApps.map { it.packageName }.toSet()

            // Packages outside the currently-loaded list (there shouldn't be any,
            // but this keeps the merge safe) are preserved as-is.
            val merged = (previousSelections - visiblePackages) + currentSelections

            repository.saveSelectedApps(merged)
            Toast.makeText(this, "${merged.size} app(s) selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        // Preserve any in-progress check changes made before filtering.
        val currentSelections = adapter.selectedPackages()
        val refreshed = filtered.map { it.copy(isSelected = it.packageName in currentSelections) }
        adapter = AppListAdapter(refreshed.toMutableList())
        recyclerApps.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
