package com.tdev.schat

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class SchatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.Debug.logLevel = LogLevel.NONE
        OneSignal.initWithContext(this, "2fa71892-7b50-477a-9d30-a84ad0411d58")
    }
}
