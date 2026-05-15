package com.mektep.app

import android.app.Application
import com.mektep.app.services.MidnightResetWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MektepApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule daily midnight reset (quest cleanup, etc.)
        MidnightResetWorker.schedule(this)
    }
}
