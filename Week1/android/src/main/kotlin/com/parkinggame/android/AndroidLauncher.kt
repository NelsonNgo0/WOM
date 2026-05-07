package com.parkinggame.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.parkinggame.ParkingGame

/**
 * AndroidLauncher.kt
 * Path: android/src/main/kotlin/com/parkinggame/android/AndroidLauncher.kt
 *
 * The Android entry point. Android Studio launches this Activity,
 * which then creates your LibGDX game.
 */
class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration().apply {
            // Use OpenGL ES 2.0 (good default for mobile games)
            useGL30 = false
            // Keep screen on while playing
            useWakelock = true
            // Hide the status bar for a more immersive feel
            useImmersiveMode = true
        }

        // This wires LibGDX to Android and starts your game
        initialize(ParkingGame(), config)
    }
}
