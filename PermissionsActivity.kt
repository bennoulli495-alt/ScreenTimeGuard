package com.familyguard.screentime.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.familyguard.screentime.R
import com.familyguard.screentime.receiver.MyDeviceAdminReceiver
import com.familyguard.screentime.util.PermissionChecks

class PermissionsActivity : AppCompatActivity() {

    private lateinit var adminComponent: ComponentName

    private lateinit var btnGrantOverlay: Button
    private lateinit var btnGrantUsageAccess: Button
    private lateinit var btnGrantDeviceAdmin: Button
    private lateinit var btnIgnoreBattery: Button
    private lateinit var btnDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnGrantOverlay = findViewById(R.id.btnGrantOverlay)
        btnGrantUsageAccess = findViewById(R.id.btnGrantUsageAccess)
        btnGrantDeviceAdmin = findViewById(R.id.btnGrantDeviceAdmin)
        btnIgnoreBattery = findViewById(R.id.btnIgnoreBattery)
        btnDone = findViewById(R.id.btnDone)

        btnGrantOverlay.setOnClickListener { requestOverlayPermission() }
        btnGrantUsageAccess.setOnClickListener { requestUsageAccessPermission() }
        btnGrantDeviceAdmin.setOnClickListener { requestDeviceAdmin() }
        btnIgnoreBattery.setOnClickListener { requestIgnoreBatteryOptimizations() }
        btnDone.setOnClickListener { finish() }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatusUi()
    }

    private fun requestOverlayPermission() {
        if (!PermissionChecks.hasOverlayPermission(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            Toast.makeText(this, "Already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestUsageAccessPermission() {
        if (!PermissionChecks.hasUsageAccessPermission(this)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            Toast.makeText(this, "Already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestDeviceAdmin() {
        if (!PermissionChecks.hasDeviceAdmin(this)) {
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
        if (!PermissionChecks.isIgnoringBatteryOptimizations(this)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
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
        btnGrantOverlay.text = if (PermissionChecks.hasOverlayPermission(this)) "✓ Overlay permission granted" else "Grant Display Over Other Apps"
        btnGrantUsageAccess.text = if (PermissionChecks.hasUsageAccessPermission(this)) "✓ Usage access granted" else "Grant Usage Access"
        btnGrantDeviceAdmin.text = if (PermissionChecks.hasDeviceAdmin(this)) "✓ Device admin enabled" else "Enable Device Admin (uninstall protection)"
        btnIgnoreBattery.text = if (PermissionChecks.isIgnoringBatteryOptimizations(this)) "✓ Battery optimization disabled" else "Disable Battery Optimization"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
