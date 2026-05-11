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

class LevelClearScreen(
    private val game: ParkingGame,
    private val levelNumber: Int
) : Screen {

    private val W = LevelData.WORLD_WIDTH
    private val H = 800f
    private val camera    = OrthographicCamera()
    private val viewport  = FitViewport(W, H, camera)
    private val font      = BitmapFont().also { it.data.setScale(3f) }
    private val smallFont = BitmapFont().also { it.data.setScale(2f) }
    private val tinyFont  = BitmapFont().also { it.data.setScale(1.6f) }

    private var btnNext = Rectangle()
    private var btnHome = Rectangle()
    private var time = 0f

    private val confetti = Array(30) {
        floatArrayOf(
            MathUtils.random(W),
            MathUtils.random(H * 0.5f, H),
            MathUtils.random(360f),
            MathUtils.random(8f, 18f),
            MathUtils.random(0.5f, 2f)
        )
    }
    private val confettiColors = arrayOf(
        UITheme.ACCENT, UITheme.ACCENT_GREEN,
        Color.CYAN, Color.WHITE, UITheme.ACCENT_RED
    )

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val sh = Gdx.graphics.height.toFloat()
                val fx = screenX.toFloat(); val fy = screenY.toFloat()
                if (UIHelper.hits(btnNext, fx, fy, sh)) { game.setScreen(GameScreen(game, levelNumber + 1)); dispose() }
                if (UIHelper.hits(btnHome, fx, fy, sh)) { game.setScreen(MenuScreen(game)); dispose() }
                return true
            }
        }
    }

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val sr = game.shapeRenderer
        sr.projectionMatrix = camera.combined
        game.batch.projectionMatrix = camera.combined

        drawConfetti(sr)
        UIHelper.drawPanel(sr, W / 2f - 175f, H * 0.25f, 350f, 420f, UITheme.BG_PANEL)
        drawStars(sr)
        drawText()
        drawButtons(sr)
    }

    private fun drawConfetti(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        confetti.forEachIndexed { i, c ->
            c[1] -= c[4] * 80f * (1f / 60f)
            if (c[1] < -20f) c[1] = H + 10f
            sr.color = confettiColors[i % confettiColors.size]
            sr.rect(c[0], c[1], c[3], c[3] * 0.5f)
        }
        sr.end()
    }

    private fun drawStars(sr: ShapeRenderer) {
        val starY = H * 0.78f
        drawStar(sr, W / 2f - 80f, starY, 28f, UITheme.ACCENT)
        drawStar(sr, W / 2f,       starY + 15f, 36f, UITheme.ACCENT)
        drawStar(sr, W / 2f + 80f, starY, 28f, UITheme.ACCENT)
    }

    private fun drawText() {
        game.batch.begin()
        UIHelper.drawCentredText(game.batch, font,      "LEVEL CLEAR!",          W / 2f, H * 0.72f, UITheme.ACCENT_GREEN)
        UIHelper.drawCentredText(game.batch, smallFont, "Level $levelNumber Complete", W / 2f, H * 0.64f, UITheme.TEXT_DIM)
        UIHelper.drawCentredText(game.batch, tinyFont,  "Perfect drift into the spot!", W / 2f, H * 0.58f, UITheme.TEXT_DIM)
        game.batch.end()
    }

    private fun drawButtons(sr: ShapeRenderer) {
        val bw = 260f; val bh = 70f; val bx = W / 2f - bw / 2f
        btnNext = UIHelper.drawButton(sr, game.batch, smallFont,
            "NEXT LEVEL", bx, H * 0.42f, bw, bh, UITheme.ACCENT_GREEN, UITheme.BG_DARK)
        btnHome = UIHelper.drawButton(sr, game.batch, smallFont,
            "HOME", bx, H * 0.30f, bw, bh, UITheme.BTN_SECONDARY, UITheme.TEXT_WHITE)
    }

    private fun drawStar(sr: ShapeRenderer, cx: Float, cy: Float, r: Float, color: Color) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = color
        val inner = r * 0.45f
        for (i in 0 until 5) {
            val outerAngle = (i * 72f - 90f) * MathUtils.degreesToRadians
            val innerAngle = outerAngle + 36f * MathUtils.degreesToRadians
            val nextOuter  = outerAngle + 72f * MathUtils.degreesToRadians
            sr.triangle(
                cx + MathUtils.cos(outerAngle) * r,  cy + MathUtils.sin(outerAngle) * r,
                cx + MathUtils.cos(innerAngle) * inner, cy + MathUtils.sin(innerAngle) * inner,
                cx + MathUtils.cos(nextOuter)  * r,  cy + MathUtils.sin(nextOuter)  * r
            )
            sr.triangle(
                cx, cy,
                cx + MathUtils.cos(outerAngle) * r, cy + MathUtils.sin(outerAngle) * r,
                cx + MathUtils.cos(innerAngle) * inner, cy + MathUtils.sin(innerAngle) * inner
            )
        }
        sr.end()
    }

    override fun resize(w: Int, h: Int) { viewport.update(w, h, true) }
    override fun hide()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun dispose() { font.dispose(); smallFont.dispose(); tinyFont.dispose() }
}