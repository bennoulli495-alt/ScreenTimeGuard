package com.familyguard.screentime.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.familyguard.screentime.receiver.DailyResetReceiver
import com.familyguard.screentime.util.Constants
import java.util.Calendar

/**
 * Schedules the single daily alarm that clears per-schedule unlock state at
 * midnight. This is the ONLY alarm the app ever registers, regardless of how
 * many schedules the user creates — schedule start/end enforcement itself is
 * handled by AppMonitorService's continuous polling, not by per-schedule
 * alarms. This guarantees no duplicate alarms can ever accumulate.
 */
object AlarmScheduler {

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyResetReceiver::class.java).apply {
            action = Constants.ALARM_ACTION_MIDNIGHT_RESET
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, Constants.ALARM_REQUEST_CODE_MIDNIGHT_RESET, intent, flags)
    }

    fun scheduleNextMidnightReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (nextMidnight.before(now)) {
            nextMidnight.add(Calendar.DAY_OF_YEAR, 1)
        }

        val pi = pendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pi)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight.timeInMillis,
            pi
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }
}
