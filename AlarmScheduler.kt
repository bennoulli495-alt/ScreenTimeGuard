package com.familyguard.screentime.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.familyguard.screentime.data.PreferenceStorage
import com.familyguard.screentime.receiver.DailyResetReceiver
import com.familyguard.screentime.util.Constants
import java.util.Calendar

/**
 * Schedules the recurring AlarmManager trigger that fires exactly at the
 * restricted window's start time each day. DailyResetReceiver handles what
 * happens when it fires, and is itself responsible for re-scheduling the
 * next day's alarm (AlarmManager one-shot exact alarms do not repeat
 * reliably across Doze, so we re-arm on every fire instead of using
 * setRepeating).
 */
object AlarmScheduler {

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyResetReceiver::class.java).apply {
            action = Constants.ALARM_ACTION_LOCK
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, Constants.ALARM_REQUEST_CODE_LOCK, intent, flags)
    }

    fun scheduleNextTrigger(context: Context) {
        val storage = PreferenceStorage(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, storage.startHour)
            set(Calendar.MINUTE, storage.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pi = pendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fall back to an inexact alarm if the user has revoked exact-alarm
            // permission; the service's own polling loop still enforces the
            // window even if this fires a little late.
            alarmManager.set(AlarmManager.RTC_WAKEUP, next.timeInMillis, pi)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            pi
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }
}
