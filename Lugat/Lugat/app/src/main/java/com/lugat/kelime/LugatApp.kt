package com.lugat.kelime

import android.app.Application
import com.lugat.kelime.notify.DailyWordNotifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LugatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyWordNotifications.createChannel(this)
    }
}
