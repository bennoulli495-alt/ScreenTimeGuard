package com.familyguard.screentime.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.Calendar

/**
 * Central repository the rest of the app talks to. Wraps PreferenceStorage
 * and adds the derived logic (is it currently inside the restricted window,
 * is a given package currently locked, etc.) so that Activities, the Service
 * and the Receivers all share one consistent decision path.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    val storage = PreferenceStorage(appContext)

    // ---------- Time window logic ----------

    fun isWithinRestrictedWindow(now: Calendar = Calendar.getInstance()): Boolean {
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = storage.startHour * 60 + storage.startMinute
        val endMinutes = storage.endHour * 60 + storage.endMinute

        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            // Window wraps past midnight, e.g. 22:00 - 02:00
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    /**
     * Whether the given foreground package should currently show the lock overlay.
     * True only if: it's a targeted package, we are inside the time window,
     * today's session has not been unlocked, and today was not "skipped".
     */
    fun shouldBlock(packageName: String, now: Calendar = Calendar.getInstance()): Boolean {
        if (packageName == appContext.packageName) return false // never lock ourselves
        if (!storage.monitoringEnabled) return false
        if (storage.isUnlockedToday) return false
        if (storage.skippedToday) return false
        if (packageName !in storage.effectiveLockedPackages()) return false
        return isWithinRestrictedWindow(now)
    }

    fun unlockSessionWithPassword(candidate: String): Boolean {
        val correct = storage.verifyPassword(candidate)
        if (correct) {
            storage.isUnlockedToday = true
        }
        return correct
    }

    // ---------- Installed app listing ----------

    fun getInstallableApps(): List<AppInfo> {
        val launchableIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedApps = packageManager.queryIntentActivities(launchableIntent, 0)
        val selected = storage.lockedPackages

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
                    isSelected = appInfo.packageName in selected
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun saveSelectedApps(packages: Set<String>) {
        storage.lockedPackages = packages
    }
}
