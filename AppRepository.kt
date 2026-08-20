package com.familyguard.screentime.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.familyguard.screentime.util.Constants
import java.util.Calendar

/**
 * Central repository the rest of the app talks to. Wraps PreferenceStorage
 * and adds the derived logic used by the UI, the foreground service, and
 * the receivers: which schedule(s) currently apply to a given app, and
 * whether a password unlocks the correct one.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    val storage = PreferenceStorage(appContext)

    // ---------- Schedule matching ----------

    /**
     * Returns the first enabled, currently-active schedule that restricts
     * [packageName] and hasn't been unlocked for today, or null if the app
     * should not be blocked right now. If more than one schedule matches
     * (overlapping schedules), the earliest-created one in the stored list
     * wins — deterministic, never crashes.
     */
    fun findBlockingSchedule(packageName: String, now: Calendar = Calendar.getInstance()): Schedule? {
        if (packageName == appContext.packageName) return null // never lock ourselves
        if (!storage.monitoringEnabled) return null

        val settingsAlsoBlocked = storage.blockSettingsApp && packageName == Constants.SETTINGS_PACKAGE

        return storage.getSchedules().firstOrNull { schedule ->
            schedule.enabled &&
                !storage.isScheduleUnlockedToday(schedule.id) &&
                (packageName in schedule.restrictedPackages || settingsAlsoBlocked) &&
                schedule.isActiveAt(now)
        }
    }

    fun unlockScheduleWithPassword(scheduleId: String, candidate: String): Boolean {
        val correct = storage.verifyPassword(candidate)
        if (correct) {
            storage.markScheduleUnlockedForToday(scheduleId)
        }
        return correct
    }

    // ---------- Installed app listing (used by AppSelectionActivity) ----------

    fun getInstallableApps(preSelected: Set<String>): List<AppInfo> {
        val launchableIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedApps = packageManager.queryIntentActivities(launchableIntent, 0)

        return resolvedApps
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != appContext.packageName }
            .map { appInfo: ApplicationInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = try {
                        packageManager.getApplicationIcon(appInfo)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    },
                    isSelected = appInfo.packageName in preSelected
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
