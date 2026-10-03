package com.volumekeys.tap

import android.graphics.drawable.Drawable

/** A lightweight representation of an installed, launchable app. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)
