package com.familyguard.screentime.util

object Constants {

    const val PREFS_NAME = "screen_time_guard_prefs"

    // Preference keys
    const val KEY_PASSWORD_HASH = "password_hash"
    const val KEY_SCHEDULES_JSON = "schedules_json"
    const val KEY_UNLOCKED_SCHEDULE_IDS = "unlocked_schedule_ids"
    const val KEY_BLOCK_SETTINGS = "block_settings"
    const val KEY_LAST_RESET_DAY = "last_reset_day_of_year"
    const val KEY_MONITORING_ENABLED = "monitoring_enabled"

    const val SETTINGS_PACKAGE = "com.android.settings"

    // Foreground service notification
    const val NOTIF_CHANNEL_ID = "screen_time_guard_channel"
    const val NOTIF_ID = 1001

    // Polling interval for foreground-app detection
    const val POLL_INTERVAL_MS = 1000L

    // Daily unlock-reset alarm (fires at midnight, clears unlockedScheduleIds
    // for the new day). Only one alarm exists regardless of how many
    // schedules the user creates.
    const val ALARM_REQUEST_CODE_MIDNIGHT_RESET = 5001
    const val ALARM_ACTION_MIDNIGHT_RESET = "com.familyguard.screentime.ACTION_MIDNIGHT_RESET"

    const val OVERLAY_UNLOCK_ACTION = "com.familyguard.screentime.ACTION_UNLOCKED"

    // Fixed recovery question shown on the "Forgot password" screen. The
    // correct answer is never stored as plaintext — only its SHA-256 hash
    // (of the trimmed, lower-cased answer) is kept here, so reading the
    // source or a decompiled APK doesn't reveal the answer directly.
    const val RECOVERY_QUESTION =
        "ScreenTimeGuard ကို ဒီနာမည်မပေးခင်က ဘယ်နာမည်နဲ့ ခေါ်ခဲ့လဲ?"
    const val RECOVERY_ANSWER_HASH =
        "0d6e8cdd4c7331d0a2dff887d2c607b454d21596d804fccba29cc5bb4b6db7be"

    const val EXTRA_SKIP_CURRENT_PASSWORD_CHECK = "extra_skip_current_password_check"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_PRESELECTED_PACKAGES = "extra_preselected_packages"
    const val EXTRA_RESULT_SELECTED_PACKAGES = "extra_result_selected_packages"
}
