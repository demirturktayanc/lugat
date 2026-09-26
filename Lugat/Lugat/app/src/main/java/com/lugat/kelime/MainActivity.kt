package com.lugat.kelime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.lugat.kelime.notify.DailyWordNotifications
import com.lugat.kelime.ui.nav.LugatRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Bildirime dokunulduğunda "Günün Kelimesi" ekranını açmak için */
    private val openWordOfDay = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent {
            LugatRoot(
                openWordOfDay = openWordOfDay.value,
                onWordOfDayHandled = { openWordOfDay.value = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(DailyWordNotifications.EXTRA_OPEN_WOTD, false) == true) {
            openWordOfDay.value = true
            intent.removeExtra(DailyWordNotifications.EXTRA_OPEN_WOTD)
        }
    }
}
