package com.lugat.kelime.ui.nav

import android.net.Uri

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DECKS = "decks"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val WOTD = "wotd"
    const val DECK = "deck/{id}"
    const val STUDY = "study/{mode}/{source}"
    const val CHOOSE_DECKS = "choose_decks"

    fun deck(id: String) = "deck/${Uri.encode(id)}"
    fun study(mode: String, source: String) = "study/$mode/${Uri.encode(source)}"
}
