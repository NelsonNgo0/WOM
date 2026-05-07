package com.parkinggame

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * ParkingGame.kt
 * Path: core/src/main/kotlin/com/parkinggame/ParkingGame.kt
 *
 * The main entry point for your LibGDX game.
 * This class is instantiated by each platform launcher (Android, Desktop).
 * It extends Game, which manages a stack of Screens.
 */
class ParkingGame : Game() {

    // SpriteBatch is used for drawing textures/sprites efficiently
    lateinit var batch: SpriteBatch

    // ShapeRenderer draws primitive shapes (rectangles, circles, lines)
    // Perfect for our minimalist art style!
    lateinit var shapeRenderer: ShapeRenderer

    override fun create() {
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        // Start on the main menu screen
        // Change to GameScreen(this, 1) to jump straight into level 1
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        // Always dispose of resources to prevent memory leaks
        batch.dispose()
        shapeRenderer.dispose()
        screen?.dispose()
    }
}
