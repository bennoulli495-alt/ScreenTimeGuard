package com.familyguard.screentime.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.familyguard.screentime.R
import com.familyguard.screentime.service.AppMonitorService
import com.familyguard.screentime.util.PermissionChecks

/**
 * Main hub screen. Kept intentionally short — schedule management,
 * permissions, and password setup each live on their own screen.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel

    private lateinit var btnPermissions: Button
    private lateinit var textPermissionsStatus: TextView
    private lateinit var btnManageSchedules: Button
    private lateinit var textSchedulesStatus: TextView
    private lateinit var btnPasswordScreen: Button
    private lateinit var textPasswordStatus: TextView
    private lateinit var switchBlockSettings: Switch
    private lateinit var btnStartService: Button
    private lateinit var textStatus: TextView

    private var hasPromptedForPermissionsThisLaunch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        bindViews()
        switchBlockSettings.isChecked = viewModel.repository.storage.blockSettingsApp
        switchBlockSettings.setOnCheckedChangeListener { _, isChecked ->
            viewModel.repository.storage.blockSettingsApp = isChecked
        }

        btnPermissions.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
        btnManageSchedules.setOnClickListener {
            startActivity(Intent(this, ScheduleListActivity::class.java))
        }
        btnPasswordScreen.setOnClickListener {
            startActivity(Intent(this, PasswordActivity::class.java))
        }
        btnStartService.setOnClickListener { validateAndStart() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatusTexts()

        // Guide the admin straight to the permissions screen the very first
        // time the app is opened, before they need to configure anything else.
        if (!hasPromptedForPermissionsThisLaunch && !PermissionChecks.allGranted(this)) {
            hasPromptedForPermissionsThisLaunch = true
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
    }

    private fun bindViews() {
        btnPermissions = findViewById(R.id.btnPermissions)
        textPermissionsStatus = findViewById(R.id.textPermissionsStatus)
        btnManageSchedules = findViewById(R.id.btnManageSchedules)
        textSchedulesStatus = findViewById(R.id.textSchedulesStatus)
        btnPasswordScreen = findViewById(R.id.btnPasswordScreen)
        textPasswordStatus = findViewById(R.id.textPasswordStatus)
        switchBlockSettings = findViewById(R.id.switchBlockSettings)
        btnStartService = findViewById(R.id.btnStartService)
        textStatus = findViewById(R.id.textStatus)
    }

    private fun refreshStatusTexts() {
        textPermissionsStatus.text = if (PermissionChecks.allGranted(this)) {
            "All permissions granted"
        } else {
            "Some permissions still needed"
        }

        val schedules = viewModel.repository.storage.getSchedules()
        val enabledCount = schedules.count { it.enabled }
        textSchedulesStatus.text = when {
            schedules.isEmpty() -> "No schedules yet"
            else -> "${schedules.size} schedule(s), $enabledCount enabled"
        }

        val hasPassword = viewModel.repository.storage.hasPasswordSet()
        btnPasswordScreen.text = if (hasPassword) "Change Password" else "Set Admin Password"
        textPasswordStatus.text = if (hasPassword) "Password is set" else "No password set yet"
    }

    private fun validateAndStart() {
        if (!PermissionChecks.allGranted(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permissions needed")
                .setMessage("Please grant all required permissions first.")
                .setPositiveButton("Open Permissions") { _, _ ->
                    startActivity(Intent(this, PermissionsActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        if (!viewModel.repository.storage.hasPasswordSet()) {
            AlertDialog.Builder(this)
                .setTitle("Password needed")
                .setMessage("Please set an admin password first.")
                .setPositiveButton("Set Password") { _, _ ->
                    startActivity(Intent(this, PasswordActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val schedules = viewModel.repository.storage.getSchedules()
        if (schedules.none { it.enabled }) {
            AlertDialog.Builder(this)
                .setTitle("No active schedules")
                .setMessage("Create at least one enabled schedule first.")
                .setPositiveButton("Manage Schedules") { _, _ ->
                    startActivity(Intent(this, ScheduleListActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val serviceIntent = Intent(this, AppMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        textStatus.text = "Monitoring is active across ${schedules.count { it.enabled }} schedule(s)."
    }
}
