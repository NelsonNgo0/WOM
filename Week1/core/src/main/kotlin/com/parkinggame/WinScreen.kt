package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.viewport.FitViewport

class WinScreen(private val game: ParkingGame) : Screen {

    private val W = LevelData.WORLD_WIDTH
    private val H = 800f
    private val camera   = OrthographicCamera()
    private val viewport = FitViewport(W, H, camera)
    private val bigFont   = BitmapFont().also { it.data.setScale(4f) }
    private val font      = BitmapFont().also { it.data.setScale(2.8f) }
    private val smallFont = BitmapFont().also { it.data.setScale(2f) }

    private var btnHome = Rectangle()
    private var btnPlay = Rectangle()
    private var time = 0f

    private val fireworks = Array(20) {
        floatArrayOf(
            MathUtils.random(40f, W - 40f),
            MathUtils.random(H * 0.4f, H),
            MathUtils.random(3f),
            MathUtils.random(1.5f, 3f)
        )
    }
    private val fwColors = arrayOf(
        UITheme.ACCENT, UITheme.ACCENT_GREEN, Color.CYAN,
        Color.WHITE, UITheme.ACCENT_RED, Color.MAGENTA
    )

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val sh = Gdx.graphics.height.toFloat()
                if (UIHelper.hits(btnPlay, screenX.toFloat(), screenY.toFloat(), sh)) {
                    game.setScreen(GameScreen(game, 1)); dispose()
                }
                if (UIHelper.hits(btnHome, screenX.toFloat(), screenY.toFloat(), sh)) {
                    game.setScreen(MenuScreen(game, showWin = true)); dispose()
                }
                return true
            }
        }
    }

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val sr = game.shapeRenderer
        sr.projectionMatrix  = camera.combined
        game.batch.projectionMatrix = camera.combined

        drawFireworks(sr)
        drawGlow(sr)
        drawTrophy(sr)
        drawText()
        drawButtons(sr)
    }

    private fun drawGlow(sr: ShapeRenderer) {
        val glow = 0.3f + 0.2f * MathUtils.sin(time * 2f)
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(UITheme.ACCENT.r, UITheme.ACCENT.g, UITheme.ACCENT.b, glow * 0.15f)
        sr.ellipse(W / 2f - 200f, H * 0.45f, 400f, 300f)
        sr.end()
    }

    private fun drawText() {
        val pulse = 0.85f + 0.15f * MathUtils.sin(time * 3f)
        game.batch.begin()
        UIHelper.drawCentredText(game.batch, bigFont, "YOU WIN!",
            W / 2f, H * 0.82f, Color(UITheme.ACCENT.r, UITheme.ACCENT.g, UITheme.ACCENT.b, pulse))
        UIHelper.drawCentredText(game.batch, font, "ALL 5 LEVELS CLEARED",
            W / 2f, H * 0.73f, UITheme.ACCENT_GREEN)
        UIHelper.drawCentredText(game.batch, smallFont, "You are the Drift Park master.",
            W / 2f, H * 0.65f, UITheme.TEXT_DIM)
        UIHelper.drawCentredText(game.batch, smallFont, "Level 5 is basically impossible",
            W / 2f, H * 0.59f, UITheme.TEXT_DIM)
        UIHelper.drawCentredText(game.batch, smallFont, "and yet here you are.",
            W / 2f, H * 0.53f, UITheme.TEXT_DIM)
        game.batch.end()
    }

    private fun drawButtons(sr: ShapeRenderer) {
        val bw = 260f; val bh = 70f; val bx = W / 2f - bw / 2f
        btnPlay = UIHelper.drawButton(sr, game.batch, smallFont,
            "PLAY AGAIN", bx, H * 0.34f, bw, bh, UITheme.ACCENT, UITheme.BG_DARK)
        btnHome = UIHelper.drawButton(sr, game.batch, smallFont,
            "HOME", bx, H * 0.22f, bw, bh, UITheme.BTN_SECONDARY, UITheme.TEXT_WHITE)
    }

    private fun drawFireworks(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        fireworks.forEachIndexed { i, fw ->
            val t = (time + fw[2]) % 2f
            if (t < 1.2f) {
                val burst = t * fw[3]
                val alpha = (1f - t / 1.2f).coerceIn(0f, 1f)
                val col = fwColors[i % fwColors.size]
                for (ray in 0 until 8) {
                    val a = ray * 45f * MathUtils.degreesToRadians
                    sr.color = Color(col.r, col.g, col.b, alpha)
                    sr.circle(
                        fw[0] + MathUtils.cos(a) * burst * 60f,
                        fw[1] + MathUtils.sin(a) * burst * 60f,
                        5f, 6
                    )
                }
            }
        }
        sr.end()
    }

    private fun drawTrophy(sr: ShapeRenderer) {
        val cx = W / 2f; val cy = H * 0.42f
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = UITheme.ACCENT
        sr.rect(cx - 30f, cy, 60f, 50f)
        sr.rect(cx - 40f, cy - 20f, 80f, 20f)
        sr.rect(cx - 15f, cy - 35f, 30f, 18f)
        sr.color = Color(UITheme.ACCENT.r * 0.7f, UITheme.ACCENT.g * 0.7f, 0.1f, 1f)
        sr.rect(cx - 50f, cy + 10f, 20f, 25f)
        sr.rect(cx + 30f, cy + 10f, 20f, 25f)
        sr.end()
    }

    override fun resize(w: Int, h: Int) { viewport.update(w, h, true) }
    override fun hide()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun dispose() { bigFont.dispose(); font.dispose(); smallFont.dispose() }
}