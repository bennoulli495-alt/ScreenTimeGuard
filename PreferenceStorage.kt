package com.familyguard.screentime.data

import android.content.Context
import android.content.SharedPreferences
import com.familyguard.screentime.util.Constants
import com.familyguard.screentime.util.PasswordUtils

/**
 * Single source of truth for all persisted configuration and session state.
 * Backed by SharedPreferences (synchronous reads, required by the polling
 * loop in AppMonitorService).
 */
class PreferenceStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Password ----------

    fun setPassword(plainTextPassword: String) {
        prefs.edit().putString(Constants.KEY_PASSWORD_HASH, PasswordUtils.hash(plainTextPassword)).apply()
    }

    fun hasPasswordSet(): Boolean = prefs.contains(Constants.KEY_PASSWORD_HASH)

    fun verifyPassword(candidate: String): Boolean {
        val storedHash = prefs.getString(Constants.KEY_PASSWORD_HASH, null)
        return PasswordUtils.matches(candidate, storedHash)
    }

    // ---------- Time window ----------

    var startHour: Int
        get() = prefs.getInt(Constants.KEY_START_HOUR, Constants.DEFAULT_START_HOUR)
        set(value) = prefs.edit().putInt(Constants.KEY_START_HOUR, value).apply()

    var startMinute: Int
        get() = prefs.getInt(Constants.KEY_START_MINUTE, Constants.DEFAULT_START_MINUTE)
        set(value) = prefs.edit().putInt(Constants.KEY_START_MINUTE, value).apply()

    var endHour: Int
        get() = prefs.getInt(Constants.KEY_END_HOUR, Constants.DEFAULT_END_HOUR)
        set(value) = prefs.edit().putInt(Constants.KEY_END_HOUR, value).apply()

    var endMinute: Int
        get() = prefs.getInt(Constants.KEY_END_MINUTE, Constants.DEFAULT_END_MINUTE)
        set(value) = prefs.edit().putInt(Constants.KEY_END_MINUTE, value).apply()

    // ---------- Session flags ----------

    var isUnlockedToday: Boolean
        get() = prefs.getBoolean(Constants.KEY_IS_UNLOCKED_TODAY, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_IS_UNLOCKED_TODAY, value).apply()

    var skipNextSession: Boolean
        get() = prefs.getBoolean(Constants.KEY_SKIP_NEXT_SESSION, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_SKIP_NEXT_SESSION, value).apply()

    /** True only for the one day a "skip" was consumed; cleared at the next window start. */
    var skippedToday: Boolean
        get() = prefs.getBoolean(Constants.KEY_SKIPPED_TODAY, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_SKIPPED_TODAY, value).apply()

    var lastResetDayOfYear: Int
        get() = prefs.getInt(Constants.KEY_LAST_RESET_DAY, -1)
        set(value) = prefs.edit().putInt(Constants.KEY_LAST_RESET_DAY, value).apply()

    var monitoringEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_MONITORING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_MONITORING_ENABLED, value).apply()

    // ---------- Locked apps ----------

    var lockedPackages: Set<String>
        get() = prefs.getStringSet(Constants.KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(Constants.KEY_LOCKED_PACKAGES, value).apply()

    var blockSettingsApp: Boolean
        get() = prefs.getBoolean(Constants.KEY_BLOCK_SETTINGS, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_BLOCK_SETTINGS, value).apply()

    /** The full set of packages currently subject to restriction. */
    fun effectiveLockedPackages(): Set<String> {
        val set = lockedPackages.toMutableSet()
        if (blockSettingsApp) set.add(Constants.SETTINGS_PACKAGE)
        return set
    }
}
