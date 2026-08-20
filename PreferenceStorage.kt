package com.familyguard.screentime.data

import android.content.Context
import android.content.SharedPreferences
import com.familyguard.screentime.util.Constants
import com.familyguard.screentime.util.PasswordUtils

/**
 * Single source of truth for all persisted configuration and session state.
 * Backed by SharedPreferences (synchronous reads, required by the polling
 * loop in AppMonitorService). Schedules are stored as a JSON array under one
 * key; each Schedule carries its own id, time range, repeat days, restricted
 * apps and enabled flag, so add/edit/delete/enable operations never disturb
 * any other schedule.
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

    // ---------- Schedules ----------

    fun getSchedules(): MutableList<Schedule> =
        ScheduleSerializer.listFromJson(prefs.getString(Constants.KEY_SCHEDULES_JSON, null))

    private fun saveSchedules(schedules: List<Schedule>) {
        prefs.edit().putString(Constants.KEY_SCHEDULES_JSON, ScheduleSerializer.listToJson(schedules)).apply()
    }

    fun getSchedule(id: String): Schedule? = getSchedules().find { it.id == id }

    fun addSchedule(schedule: Schedule) {
        val list = getSchedules()
        list.add(schedule)
        saveSchedules(list)
    }

    /** Replaces the schedule with the same id. Every other schedule is left untouched. */
    fun updateSchedule(updated: Schedule) {
        val list = getSchedules()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            list[index] = updated
        } else {
            list.add(updated)
        }
        saveSchedules(list)
    }

    /** Deletes only the given schedule and its unlock state. Every other schedule is unaffected. */
    fun deleteSchedule(id: String) {
        val list = getSchedules()
        list.removeAll { it.id == id }
        saveSchedules(list)

        val unlocked = unlockedScheduleIds.toMutableSet()
        if (unlocked.remove(id)) {
            unlockedScheduleIds = unlocked
        }
    }

    fun setScheduleEnabled(id: String, enabled: Boolean) {
        val list = getSchedules()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            list[index] = list[index].copy(enabled = enabled)
            saveSchedules(list)
        }
    }

    // ---------- Per-schedule daily unlock state ----------

    /** Schedule IDs unlocked for the remainder of today. Cleared at midnight. */
    var unlockedScheduleIds: Set<String>
        get() = prefs.getStringSet(Constants.KEY_UNLOCKED_SCHEDULE_IDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(Constants.KEY_UNLOCKED_SCHEDULE_IDS, value).apply()

    fun markScheduleUnlockedForToday(scheduleId: String) {
        unlockedScheduleIds = unlockedScheduleIds + scheduleId
    }

    fun isScheduleUnlockedToday(scheduleId: String): Boolean =
        scheduleId in unlockedScheduleIds

    var lastResetDayOfYear: Int
        get() = prefs.getInt(Constants.KEY_LAST_RESET_DAY, -1)
        set(value) = prefs.edit().putInt(Constants.KEY_LAST_RESET_DAY, value).apply()

    var monitoringEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_MONITORING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_MONITORING_ENABLED, value).apply()

    // ---------- Global options ----------

    var blockSettingsApp: Boolean
        get() = prefs.getBoolean(Constants.KEY_BLOCK_SETTINGS, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_BLOCK_SETTINGS, value).apply()
}
