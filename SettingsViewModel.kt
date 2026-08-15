package com.familyguard.screentime.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.familyguard.screentime.data.AppInfo
import com.familyguard.screentime.data.AppRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(application)

    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps

    fun loadApps() {
        _apps.value = repository.getInstallableApps()
    }

    fun saveSelectedApps(packages: Set<String>) {
        repository.saveSelectedApps(packages)
    }

    fun savePassword(plainText: String) {
        repository.storage.setPassword(plainText)
    }

    fun saveStartTime(hour: Int, minute: Int) {
        repository.storage.startHour = hour
        repository.storage.startMinute = minute
    }

    fun saveEndTime(hour: Int, minute: Int) {
        repository.storage.endHour = hour
        repository.storage.endMinute = minute
    }

    fun setSkipNextSession(skip: Boolean) {
        repository.storage.skipNextSession = skip
    }

    fun setBlockSettingsApp(block: Boolean) {
        repository.storage.blockSettingsApp = block
    }
}
