package com.familyguard.screentime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.familyguard.screentime.data.PreferenceStorage
import com.familyguard.screentime.scheduler.AlarmScheduler
import java.util.Calendar

/**
 * Fires once daily at midnight. Clears every schedule's "unlocked for
 * today" state so each schedule re-engages for the new day, then re-arms
 * itself for the following midnight (a one-shot exact alarm does not repeat
 * on its own).
 */
class DailyResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val storage = PreferenceStorage(context)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        if (storage.lastResetDayOfYear != today) {
            storage.unlockedScheduleIds = emptySet()
            storage.lastResetDayOfYear = today
        }

        AlarmScheduler.scheduleNextMidnightReset(context)
    }
}
