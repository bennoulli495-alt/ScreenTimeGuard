# Screen Time Guard

A shared-device screen-time restriction app for Android 8.0+. Blocks selected
apps (and optionally the Settings app) during a daily time window, with a
password to unlock a session and a "skip next session" override.

The app is transparent by design: it appears in the launcher and app list
under the name "Screen Time Guard," and every restriction it enforces
(uninstall protection, Settings blocking) can be turned off again from
Android's own Settings menu by whoever holds the device.

## Package layout

```
app/src/main/java/com/familyguard/screentime/
  App.kt                          Application class, sets up notification channel
  util/
    Constants.kt                  Shared keys and defaults
    PasswordUtils.kt              SHA-256 password hashing
  data/
    PreferenceStorage.kt          SharedPreferences wrapper (all persisted state)
    AppInfo.kt                    Installed-app UI model
    AppRepository.kt              Time-window logic + installed app queries
  receiver/
    MyDeviceAdminReceiver.kt      Device Admin callbacks
    BootReceiver.kt               Restarts service after reboot
    DailyResetReceiver.kt         Daily lock/skip logic (AlarmManager target)
  scheduler/
    AlarmScheduler.kt             Schedules the exact daily alarm
  service/
    AppMonitorService.kt          Foreground service, polls current app
    LockScreenOverlayManager.kt   WindowManager full-screen password overlay
  ui/
    SettingsActivity.kt           Configuration screen + permission requests
    SettingsViewModel.kt          MVVM ViewModel
    AppListAdapter.kt             RecyclerView adapter for app selection
```

## First-run setup (in order)

1. Install the APK and open **Screen Time Guard**.
2. Tap **Grant Display Over Other Apps** → enable the toggle for this app.
3. Tap **Grant Usage Access** → enable "Screen Time Guard" (this lets the
   service see which app is currently in the foreground).
4. Tap **Enable Device Admin** → confirm. This adds a step before the app
   can be uninstalled or force-stopped; either sibling can undo it later at
   **Settings → Security → Device admin apps → Deactivate**.
5. Tap **Disable Battery Optimization** → allow. Realme's ColorOS-based
   battery manager is aggressive about killing background apps; without
   this, the monitor service may stop working after a few hours.
6. Set the **start/end time** (defaults to 15:00–20:00).
7. Set an **admin password** — this is hashed (SHA-256) before it's saved,
   so it isn't stored as readable text on the device.
8. Select the apps to restrict from the list (e.g. TikTok). Optionally
   toggle **"Also restrict Settings app."**
9. Tap **Save & Start Monitoring**.

### One more Realme-specific step (not automatable from within the app)

Go to **Settings → App management → Screen Time Guard → Battery** and set it
to **"Allow background activity" / disable "Optimize battery use."** Also
check **Settings → Startup manager** and allow Screen Time Guard to
auto-start. Without this, ColorOS can silently kill the foreground service
regardless of the in-app battery toggle.

## How the daily cycle works

- `AlarmScheduler` sets an exact alarm (`setExactAndAllowWhileIdle`) for the
  configured start time (e.g. 15:00) every day.
- When it fires, `DailyResetReceiver` runs:
  - If **Skip Next Session** was on, that day is marked skipped (no
    blocking) and the toggle is cleared, so the day after reverts to
    normal restriction.
  - Otherwise, `isUnlockedToday` resets to `false`, re-locking the session.
  - The alarm is then re-armed for the following day.
- `AppMonitorService` polls the foreground app roughly once per second via
  `UsageStatsManager` and shows the full-screen overlay whenever a
  restricted app is in front during the window and the session hasn't been
  unlocked (or skipped) for the day.
- Entering the correct password unlocks the session for the rest of that
  day; it does not need to be re-entered until the next reset.

## Known, honest limitations

- **Device Admin ≠ unremovable.** Android does not allow third-party apps to
  fully block uninstallation without full "Device Owner" provisioning
  (which requires setting the app up as a Managed Device at factory-reset
  time, not something installable after the fact). What this app provides
  is a deliberate speed bump, not a hard lock.
- **UsageStatsManager polling has a small delay** (up to ~1 second), so the
  overlay may take a moment to appear after a locked app is opened.
- **Manufacturer battery managers** (ColorOS/Realme UI, MIUI, etc.) can kill
  background services despite `START_STICKY` and the persistent
  notification; the Realme-specific steps above are the practical fix.
- If **exact-alarm permission** is later revoked in Settings, the daily
  reset falls back to an inexact alarm, which may fire a little late; the
  service's own polling still enforces the time window in the meantime.

## Building with Codemagic (matches your existing GitHub-web-upload workflow)

The included `codemagic.yaml` first checks whether `app/build.gradle`
already exists at the expected path. If you upload the files through
GitHub's web UI (which flattens folders), it reconstructs the full project
structure and picks the newest "(n)" duplicate of any re-uploaded file,
the same pattern used in your earlier build steps. Then it generates the
Gradle wrapper if missing and runs `assembleDebug`, producing an installable
`app-debug.apk` as a build artifact.
