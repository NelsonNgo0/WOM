package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/**
 * GameSettings.kt — persists user preferences across sessions
 */
object GameSettings {
    private val prefs: Preferences by lazy { Gdx.app.getPreferences("DriftParkSettings") }

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) { prefs.putBoolean("sound", v); prefs.flush() }

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music", true)
        set(v) { prefs.putBoolean("music", v); prefs.flush() }
}
