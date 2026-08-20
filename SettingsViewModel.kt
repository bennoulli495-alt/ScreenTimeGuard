package com.familyguard.screentime.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.familyguard.screentime.data.AppRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AppRepository(application)
}
