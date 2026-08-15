package com.familyguard.screentime.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    var isSelected: Boolean = false
)
