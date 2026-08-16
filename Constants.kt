package com.familyguard.screentime.util

object Constants {

    const val PREFS_NAME = "screen_time_guard_prefs"

    // Preference keys
    const val KEY_PASSWORD_HASH = "password_hash"
    const val KEY_START_HOUR = "start_hour"
    const val KEY_START_MINUTE = "start_minute"
    const val KEY_END_HOUR = "end_hour"
    const val KEY_END_MINUTE = "end_minute"
    const val KEY_IS_UNLOCKED_TODAY = "is_unlocked_today"
    const val KEY_SKIP_NEXT_SESSION = "skip_next_session"
    const val KEY_SKIPPED_TODAY = "skipped_today"
    const val KEY_LOCKED_PACKAGES = "locked_packages"
    const val KEY_BLOCK_SETTINGS = "block_settings"
    const val KEY_LAST_RESET_DAY = "last_reset_day_of_year"
    const val KEY_MONITORING_ENABLED = "monitoring_enabled"

    // Defaults matching the spec: 3:00 PM - 8:00 PM
    const val DEFAULT_START_HOUR = 15
    const val DEFAULT_START_MINUTE = 0
    const val DEFAULT_END_HOUR = 20
    const val DEFAULT_END_MINUTE = 0

    const val SETTINGS_PACKAGE = "com.android.settings"

    // Foreground service notification
    const val NOTIF_CHANNEL_ID = "screen_time_guard_channel"
    const val NOTIF_ID = 1001

    // Polling interval for foreground-app detection
    const val POLL_INTERVAL_MS = 1000L

    // Broadcast / alarm request codes
    const val ALARM_REQUEST_CODE_LOCK = 5001
    const val ALARM_ACTION_LOCK = "com.familyguard.screentime.ACTION_LOCK_WINDOW_START"

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
}
