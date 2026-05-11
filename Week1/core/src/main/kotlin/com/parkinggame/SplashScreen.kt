package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * SplashScreen.kt
 *
 * Shows v02-splash.png for DISPLAY_TIME seconds then fades out to MenuScreen.
 * Place v02-splash.png in the assets/ folder.
 */
class SplashScreen(private val game: ParkingGame) : Screen {

    private val camera   = OrthographicCamera()
    private val viewport = FitViewport(1080f, 1920f, camera)

    private val texture  = Texture(Gdx.files.internal("v02-splash.png"))
    private val sprite   = Sprite(texture).also {
        it.setSize(1080f, 1920f)
        it.setPosition(0f, 0f)
    }

    private var timer = 0f
    private var alpha = 0f          // Current opacity (0 = invisible, 1 = fully visible)

    companion object {
        const val FADE_IN_TIME  = 0.6f   // Seconds to fade in
        const val DISPLAY_TIME  = 2.0f   // Seconds fully visible
        const val FADE_OUT_TIME = 0.6f   // Seconds to fade out
        const val TOTAL_TIME    = FADE_IN_TIME + DISPLAY_TIME + FADE_OUT_TIME
    }

    override fun show() {}

    override fun render(delta: Float) {
        timer += delta

        // Calculate alpha based on where we are in the timeline
        alpha = when {
            timer < FADE_IN_TIME ->
                // Fading in
                timer / FADE_IN_TIME
            timer < FADE_IN_TIME + DISPLAY_TIME ->
                // Fully visible
                1f
            timer < TOTAL_TIME ->
                // Fading out
                1f - (timer - FADE_IN_TIME - DISPLAY_TIME) / FADE_OUT_TIME
            else -> {
                // Done — go to menu
                game.setScreen(MenuScreen(game))
                dispose()
                return
            }
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        sprite.setAlpha(alpha.coerceIn(0f, 1f))
        sprite.draw(game.batch)
        game.batch.end()
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
    override fun hide()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun dispose() { texture.dispose() }
}