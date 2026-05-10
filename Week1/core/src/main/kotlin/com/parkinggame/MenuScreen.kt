package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * MenuScreen.kt
 * Path: core/src/main/kotlin/com/parkinggame/MenuScreen.kt
 *
 * Simple main menu with:
 * - Title
 * - "Tap to Play" prompt
 * - Win message if showWin = true
 */
class MenuScreen(
    private val game: ParkingGame,
    private val showWin: Boolean = false
) : Screen {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(LevelData.WORLD_WIDTH, LevelData.WORLD_HEIGHT, camera)
    private val font = BitmapFont().also { it.data.setScale(2.8f) }
    private val layout = GlyphLayout()

    // Pulsing animation
    private var time = 0f

    override fun show() {
        // Use a simple input adapter that starts the game on any touch
        Gdx.input.inputProcessor = object : com.badlogic.gdx.InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                game.setScreen(GameScreen(game, 1))
                dispose()
                return true
            }
        }
    }

    override fun render(delta: Float) {
        time += delta

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val sr = game.shapeRenderer
        val ww = LevelData.WORLD_WIDTH
        val wh = LevelData.WORLD_HEIGHT

        sr.projectionMatrix = camera.combined

        // Draw a stylised car silhouette in the background
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.85f, 0.2f, 0.2f, 0.3f)
        sr.rect(ww / 2f - 40f, wh / 2f - 100f, 80f, 130f)
        sr.end()

        // Title
        game.batch.projectionMatrix = camera.combined
        game.batch.begin()

        // Title text
        font.color = Color.WHITE
        val title = if (showWin) "YOU PARKED THEM ALL!" else "DRIFT PARK"
        layout.setText(font, title)
        font.draw(game.batch, title, (ww - layout.width) / 2f, wh * 0.72f)

        // Subtitle
        font.color = Color(0.7f, 0.7f, 0.7f, 1f)
        val sub = "5 Levels • Arcade Drifting"
        layout.setText(font, sub)
        font.draw(game.batch, sub, (ww - layout.width) / 2f, wh * 0.65f)

        // Pulsing tap prompt
        val pulse = 0.6f + 0.4f * Math.sin(time * 3.0).toFloat()
        font.color = Color(1f, 0.9f, 0.3f, pulse)
        val tap = if (showWin) "Tap to Play Again" else "Tap to Play"
        layout.setText(font, tap)
        font.draw(game.batch, tap, (ww - layout.width) / 2f, wh * 0.4f)

        // Controls hint
        font.color = Color(0.5f, 0.5f, 0.5f, 1f)
        font.draw(game.batch, "LEFT = BRAKE    RIGHT = STEER", 40f, wh * 0.2f)

        game.batch.end()
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { font.dispose() }
}