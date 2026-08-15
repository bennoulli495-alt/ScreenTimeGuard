package com.familyguard.screentime.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyguard.screentime.R
import com.familyguard.screentime.receiver.MyDeviceAdminReceiver
import com.familyguard.screentime.scheduler.AlarmScheduler
import com.familyguard.screentime.service.AppMonitorService

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var adapter: AppListAdapter
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private lateinit var btnGrantOverlay: Button
    private lateinit var btnGrantUsageAccess: Button
    private lateinit var btnGrantDeviceAdmin: Button
    private lateinit var btnIgnoreBattery: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var editPassword: EditText
    private lateinit var btnSavePassword: Button
    private lateinit var switchSkipNext: Switch
    private lateinit var switchBlockSettings: Switch
    private lateinit var recyclerApps: RecyclerView
    private lateinit var btnStartService: Button
    private lateinit var textStatus: TextView

    private var startHour = 15
    private var startMinute = 0
    private var endHour = 20
    private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        bindViews()
        restoreState()
        setupAppList()
        setupListeners()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatusUi()
    }

    private fun bindViews() {
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay)
        btnGrantUsageAccess = findViewById(R.id.btnGrantUsageAccess)
        btnGrantDeviceAdmin = findViewById(R.id.btnGrantDeviceAdmin)
        btnIgnoreBattery = findViewById(R.id.btnIgnoreBattery)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        editPassword = findViewById(R.id.editPassword)
        btnSavePassword = findViewById(R.id.btnSavePassword)
        switchSkipNext = findViewById(R.id.switchSkipNext)
        switchBlockSettings = findViewById(R.id.switchBlockSettings)
        recyclerApps = findViewById(R.id.recyclerApps)
        btnStartService = findViewById(R.id.btnStartService)
        textStatus = findViewById(R.id.textStatus)
    }

    private fun restoreState() {
        val storage = viewModel.repository.storage
        startHour = storage.startHour
        startMinute = storage.startMinute
        endHour = storage.endHour
        endMinute = storage.endMinute
        btnStartTime.text = "%02d:%02d".format(startHour, startMinute)
        btnEndTime.text = "%02d:%02d".format(endHour, endMinute)
        switchSkipNext.isChecked = storage.skipNextSession
        switchBlockSettings.isChecked = storage.blockSettingsApp
    }

    private fun setupAppList() {
        adapter = AppListAdapter(mutableListOf())
        recyclerApps.layoutManager = LinearLayoutManager(this)
        recyclerApps.adapter = adapter

        viewModel.apps.observe(this) { apps ->
            adapter = AppListAdapter(apps.toMutableList())
            recyclerApps.adapter = adapter
        }
        viewModel.loadApps()
    }

    private fun setupListeners() {
        btnGrantOverlay.setOnClickListener { requestOverlayPermission() }
        btnGrantUsageAccess.setOnClickListener { requestUsageAccessPermission() }
        btnGrantDeviceAdmin.setOnClickListener { requestDeviceAdmin() }
        btnIgnoreBattery.setOnClickListener { requestIgnoreBatteryOptimizations() }

        btnStartTime.setOnClickListener {
            showTimePicker(startHour, startMinute) { h, m ->
                startHour = h; startMinute = m
                btnStartTime.text = "%02d:%02d".format(h, m)
                viewModel.saveStartTime(h, m)
                AlarmScheduler.scheduleNextTrigger(this)
            }
        }
        btnEndTime.setOnClickListener {
            showTimePicker(endHour, endMinute) { h, m ->
                endHour = h; endMinute = m
                btnEndTime.text = "%02d:%02d".format(h, m)
                viewModel.saveEndTime(h, m)
            }
        }

        btnSavePassword.setOnClickListener {
            val pwd = editPassword.text.toString()
            if (pwd.length < 4) {
                Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.savePassword(pwd)
                editPassword.text.clear()
                Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show()
            }
        }

        switchSkipNext.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSkipNextSession(isChecked)
        }

        switchBlockSettings.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBlockSettingsApp(isChecked)
        }

        btnStartService.setOnClickListener { validateAndStart() }
    }

    private fun showTimePicker(hour: Int, minute: Int, onSet: (Int, Int) -> Unit) {
        TimePickerDialog(this, { _, h, m -> onSet(h, m) }, hour, minute, true).show()
    }

    // ---------- Permission helpers ----------

    private fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(this)

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasDeviceAdmin(): Boolean =
        devicePolicyManager.isAdminActive(adminComponent)

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestOverlayPermission() {
        if (!hasOverlayPermission()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestUsageAccessPermission() {
        if (!hasUsageAccessPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            Toast.makeText(this, "Already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestDeviceAdmin() {
        if (!hasDeviceAdmin()) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Adds a confirmation step before this app can be uninstalled or force-stopped."
                )
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Already unrestricted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    private fun refreshPermissionStatusUi() {
        btnGrantOverlay.text = if (hasOverlayPermission()) "✓ Overlay permission granted" else "Grant Display Over Other Apps"
        btnGrantUsageAccess.text = if (hasUsageAccessPermission()) "✓ Usage access granted" else "Grant Usage Access"
        btnGrantDeviceAdmin.text = if (hasDeviceAdmin()) "✓ Device admin enabled" else "Enable Device Admin (uninstall protection)"
        btnIgnoreBattery.text = if (isIgnoringBatteryOptimizations()) "✓ Battery optimization disabled" else "Disable Battery Optimization"
    }

    // ---------- Save & start ----------

    private fun validateAndStart() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Please grant the overlay permission first", Toast.LENGTH_LONG).show()
            return
        }
        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please grant usage access first", Toast.LENGTH_LONG).show()
            return
        }
        if (!viewModel.repository.storage.hasPasswordSet()) {
            Toast.makeText(this, "Please set an admin password first", Toast.LENGTH_LONG).show()
            return
        }

        val selected = adapter.selectedPackages()
        if (selected.isEmpty() && !switchBlockSettings.isChecked) {
            AlertDialog.Builder(this)
                .setTitle("No apps selected")
                .setMessage("Select at least one app to restrict, or enable Settings-app blocking.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        viewModel.saveSelectedApps(selected)

        val serviceIntent = Intent(this, AppMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        AlarmScheduler.scheduleNextTrigger(this)

        textStatus.text = "Monitoring is active. Restricted window: " +
            "%02d:%02d - %02d:%02d daily.".format(startHour, startMinute, endHour, endMinute)
        Toast.makeText(this, "Screen Time Guard started", Toast.LENGTH_SHORT).show()
    }
}
