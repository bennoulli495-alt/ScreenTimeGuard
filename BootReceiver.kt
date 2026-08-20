package com.familyguard.screentime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.familyguard.screentime.data.PreferenceStorage
import com.familyguard.screentime.scheduler.AlarmScheduler
import com.familyguard.screentime.service.AppMonitorService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val storage = PreferenceStorage(context)
        if (!storage.monitoringEnabled) return

        // Re-arm the daily midnight unlock-reset alarm.
        AlarmScheduler.scheduleNextMidnightReset(context)

        // Restart the foreground monitor service.
        val serviceIntent = Intent(context, AppMonitorService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
