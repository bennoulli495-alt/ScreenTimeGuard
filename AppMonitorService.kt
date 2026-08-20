package com.familyguard.screentime.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppRepository
import com.familyguard.screentime.scheduler.AlarmScheduler
import com.familyguard.screentime.ui.SettingsActivity
import com.familyguard.screentime.util.Constants
import java.util.Calendar

/**
 * Continuously-running foreground service. Polls UsageStatsManager to find
 * the current foreground package and, if that package is targeted and we
 * are inside the restricted window, shows the full-screen lock overlay.
 *
 * A persistent low-priority notification is required by Android for any
 * long-running foreground service; it also makes the service far less
 * likely to be killed by the OS's background-process reaper.
 */
class AppMonitorService : Service() {

    private lateinit var repository: AppRepository
    private lateinit var overlayManager: LockScreenOverlayManager
    private lateinit var usageStatsManager: UsageStatsManager
    private val handler = Handler(Looper.getMainLooper())

    private var lastCheckedPackage: String? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            pollForegroundApp()
            handler.postDelayed(this, Constants.POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(this)
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        overlayManager = LockScreenOverlayManager(
            context = this,
            repository = repository,
            onUnlocked = { lastCheckedPackage = null }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()
        repository.storage.monitoringEnabled = true
        AlarmScheduler.scheduleNextMidnightReset(this)

        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)

        // START_STICKY: ask the OS to recreate the service if it is killed
        // due to memory pressure.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        overlayManager.hide()
    }

    // ---------- Core polling logic ----------

    private fun pollForegroundApp() {
        val foregroundPackage = queryForegroundPackage() ?: return

        if (foregroundPackage == lastCheckedPackage && overlayManager.isShowing) {
            // Still the same locked app with the overlay already up; nothing to do.
            return
        }
        lastCheckedPackage = foregroundPackage

        val now = Calendar.getInstance()
        val blockingSchedule = repository.findBlockingSchedule(foregroundPackage, now)
        if (blockingSchedule != null) {
            overlayManager.show(blockingSchedule)
        } else if (overlayManager.isShowing) {
            // The user navigated away from the locked app on their own,
            // e.g. pressed Home. Hide the overlay so it doesn't cover
            // whatever they switched to; it will reappear if they return
            // to a locked app.
            overlayManager.hide()
        }
    }

    /**
     * Uses UsageStatsManager to determine the most recently resumed app,
     * which is the standard replacement for the deprecated
     * ActivityManager#getRunningTasks() approach.
     */
    private fun queryForegroundPackage(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000 // look back 10 seconds

        val events = usageStatsManager.queryEvents(beginTime, endTime)
        var lastResumedPackage: String? = null
        val event = android.app.usage.UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastResumedPackage = event.packageName
            }
        }
        return lastResumedPackage
    }

    // ---------- Foreground notification ----------

    private fun startForegroundWithNotification() {
        val openAppIntent = Intent(this, SettingsActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIF_CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Constants.NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(Constants.NOTIF_ID, notification)
        }
    }
}
