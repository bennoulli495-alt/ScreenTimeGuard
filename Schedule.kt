package com.familyguard.screentime.data

import java.util.Calendar
import java.util.UUID

/**
 * A single, independently-managed restriction schedule (e.g. "School",
 * "Study", "Sleep"). Multiple schedules can exist and run at the same time;
 * each is fully self-contained (own days, own time range, own restricted
 * apps, own enabled state) and identified by a stable [id] so that editing
 * or deleting one never touches another.
 */
data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var startHour: Int,
    var startMinute: Int,
    var endHour: Int,
    var endMinute: Int,
    /** Calendar.SUNDAY(1) .. Calendar.SATURDAY(7). Empty = never triggers. */
    var repeatDays: Set<Int> = emptySet(),
    var restrictedPackages: Set<String> = emptySet(),
    var enabled: Boolean = true
) {
    /**
     * Whether this schedule's restricted window covers [now]. Handles
     * overnight windows (e.g. 22:00 -> 06:00) by also checking whether
     * yesterday's weekday was a repeat day when the window has wrapped past
     * midnight. Deterministic for overlapping schedules: each schedule is
     * evaluated independently.
     */
    fun isActiveAt(now: Calendar): Boolean {
        if (!enabled || repeatDays.isEmpty()) return false

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute
        val todayDow = now.get(Calendar.DAY_OF_WEEK)

        return if (startMinutes <= endMinutes) {
            todayDow in repeatDays && nowMinutes in startMinutes until endMinutes
        } else {
            // Window wraps past midnight (e.g. Sleep: 22:00 - 06:00).
            val yesterdayDow = if (todayDow == Calendar.SUNDAY) Calendar.SATURDAY else todayDow - 1
            (todayDow in repeatDays && nowMinutes >= startMinutes) ||
                (yesterdayDow in repeatDays && nowMinutes < endMinutes)
        }
    }

    fun timeRangeLabel(): String =
        "%02d:%02d - %02d:%02d".format(startHour, startMinute, endHour, endMinute)

    fun daysLabel(): String {
        if (repeatDays.isEmpty()) return "No days selected"
        val order = listOf(
            Calendar.SUNDAY to "Sun", Calendar.MONDAY to "Mon", Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed", Calendar.THURSDAY to "Thu", Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )
        return order.filter { it.first in repeatDays }.joinToString(", ") { it.second }
    }
}
