package app.tisimai.mektep

import android.app.Application
import app.tisimai.mektep.services.MidnightResetWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MektepApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule daily midnight reset (quest cleanup, etc.)
        MidnightResetWorker.schedule(this)
    }
}
