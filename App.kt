package com.familyguard.screentime

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.familyguard.screentime.util.Constants

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(Constants.NOTIF_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    Constants.NOTIF_CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    // Low importance + no badge keeps this unobtrusive while still
                    // satisfying the requirement that a foreground service must
                    // show a persistent notification (this also protects the
                    // service from being silently killed by the OS).
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
