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

class LoseScreen(
    private val game: ParkingGame,
    private val levelNumber: Int
) : Screen {

    private val W = LevelData.WORLD_WIDTH
    private val H = 800f
    private val camera   = OrthographicCamera()
    private val viewport = FitViewport(W, H, camera)
    private val font      = BitmapFont().also { it.data.setScale(3f) }
    private val smallFont = BitmapFont().also { it.data.setScale(2f) }

    private var btnRetry = Rectangle()
    private var btnHome  = Rectangle()
    private var time = 0f

    private val sparks = Array(12) {
        floatArrayOf(
            W / 2f + MathUtils.random(-80f, 80f),
            H * 0.55f + MathUtils.random(-40f, 40f),
            MathUtils.random(360f),
            MathUtils.random(40f, 100f)
        )
    }

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val sh = Gdx.graphics.height.toFloat()
                val fx = screenX.toFloat(); val fy = screenY.toFloat()
                if (UIHelper.hits(btnRetry, fx, fy, sh)) { game.setScreen(GameScreen(game, levelNumber)); dispose() }
                if (UIHelper.hits(btnHome,  fx, fy, sh)) { game.setScreen(MenuScreen(game)); dispose() }
                return true
            }
        }
    }

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0.06f, 0.04f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val sr = game.shapeRenderer
        sr.projectionMatrix = camera.combined
        game.batch.projectionMatrix = camera.combined

        drawSparks(sr)
        UIHelper.drawPanel(sr, W / 2f - 170f, H * 0.28f, 340f, 380f, UITheme.BG_PANEL)
        drawText()
        drawButtons(sr)
    }

    private fun drawSparks(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sparks.forEach { s ->
            val pulse = 0.4f + 0.6f * MathUtils.sin(time * 3f + s[2])
            sr.color = Color(1f, 0.4f + pulse * 0.3f, 0.1f, pulse * 0.8f)
            sr.circle(
                s[0] + MathUtils.cos(s[2]) * time * 20f,
                s[1] + MathUtils.sin(s[2]) * time * 15f,
                s[3] * (0.3f + 0.7f * pulse) * 0.15f, 8
            )
        }
        sr.end()
    }

    private fun drawText() {
        game.batch.begin()
        UIHelper.drawCentredText(game.batch, font, "CRASHED!", W / 2f, H * 0.78f, UITheme.ACCENT_RED)
        UIHelper.drawCentredText(game.batch, smallFont, "Level $levelNumber", W / 2f, H * 0.70f, UITheme.TEXT_DIM)
        game.batch.end()
    }

    private fun drawButtons(sr: ShapeRenderer) {
        val bw = 260f; val bh = 70f; val bx = W / 2f - bw / 2f
        btnRetry = UIHelper.drawButton(sr, game.batch, smallFont,
            "TRY AGAIN", bx, H * 0.46f, bw, bh, UITheme.ACCENT, UITheme.BG_DARK)
        btnHome = UIHelper.drawButton(sr, game.batch, smallFont,
            "HOME", bx, H * 0.34f, bw, bh, UITheme.BTN_SECONDARY, UITheme.TEXT_WHITE)
    }

    override fun resize(w: Int, h: Int) { viewport.update(w, h, true) }
    override fun hide()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun dispose() { font.dispose(); smallFont.dispose() }
}