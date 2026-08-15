package com.familyguard.screentime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.familyguard.screentime.data.PreferenceStorage
import com.familyguard.screentime.scheduler.AlarmScheduler
import java.util.Calendar

/**
 * Fires once a day at the restricted window's start time.
 *
 * Responsibilities:
 *  1. Reset isUnlockedToday back to false so the lock re-engages for the
 *     new session, UNLESS "skip next session" was requested.
 *  2. If skipNextSession was true, consume it for today only (mark
 *     skippedToday = true) and clear the flag so it does NOT apply again
 *     the following day.
 *  3. Re-arm the alarm for the next day (setExactAndAllowWhileIdle is a
 *     one-shot alarm, so this must be done on every fire and also after
 *     boot / time-window changes).
 */
class DailyResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val storage = PreferenceStorage(context)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        // Avoid double-processing if the receiver is triggered twice for the same day
        if (storage.lastResetDayOfYear == today) {
            AlarmScheduler.scheduleNextTrigger(context)
            return
        }

        if (storage.skipNextSession) {
            // Skip is consumed for today only, then cleared so tomorrow is
            // restricted again by default.
            storage.skippedToday = true
            storage.skipNextSession = false
        } else {
            storage.skippedToday = false
        }

        // New session begins locked (unless skipped above).
        storage.isUnlockedToday = false
        storage.lastResetDayOfYear = today

        AlarmScheduler.scheduleNextTrigger(context)
    }
}
