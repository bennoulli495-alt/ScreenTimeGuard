package com.familyguard.screentime.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppInfo
import com.familyguard.screentime.data.AppRepository
import com.familyguard.screentime.util.Constants

/**
 * Standalone, schedule-agnostic screen for picking a set of apps. The caller
 * (ScheduleEditActivity) passes in the currently-selected packages and
 * receives the new selection back as an activity result — this screen never
 * touches storage directly, so it works equally well for a brand-new,
 * not-yet-saved schedule as for an existing one.
 */
class AppSelectionActivity : AppCompatActivity() {

    private lateinit var repository: AppRepository
    private lateinit var recyclerApps: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var btnSaveSelection: Button

    private var allApps: List<AppInfo> = emptyList()
    private val selectedPackages: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        repository = AppRepository(this)
        val preSelected = intent.getStringArrayExtra(Constants.EXTRA_PRESELECTED_PACKAGES)?.toSet() ?: emptySet()
        selectedPackages.addAll(preSelected)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerApps = findViewById(R.id.recyclerApps)
        editSearch = findViewById(R.id.editSearch)
        btnSaveSelection = findViewById(R.id.btnSaveSelection)

        recyclerApps.layoutManager = LinearLayoutManager(this)

        allApps = repository.getInstallableApps(preSelected)
        recyclerApps.adapter = AppListAdapter(allApps, selectedPackages)

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSaveSelection.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra(Constants.EXTRA_RESULT_SELECTED_PACKAGES, selectedPackages.toTypedArray())
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        recyclerApps.adapter = AppListAdapter(filtered, selectedPackages)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
