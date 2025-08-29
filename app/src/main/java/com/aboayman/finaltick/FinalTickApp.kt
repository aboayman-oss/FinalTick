package com.aboayman.finaltick

import android.app.Application
import com.google.android.material.color.DynamicColors

class FinalTickApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply Material You dynamic color on supported devices (Android 12+)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}

