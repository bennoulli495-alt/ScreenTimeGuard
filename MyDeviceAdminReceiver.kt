package com.familyguard.screentime.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Standard Device Admin receiver. When active, Android requires an extra
 * "Deactivate this device admin app" step (Settings > Security > Device
 * admin apps) before the app can be uninstalled or force-stopped, which
 * prevents accidental or casual removal. The device's actual owner can
 * always deactivate it and uninstall normally — this is a speed bump, not
 * a lock-out.
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Device admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Device admin disabled", Toast.LENGTH_SHORT).show()
    }
}
