package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.viewport.FitViewport

class MenuScreen(
    private val game: ParkingGame,
    val showWin: Boolean = false
) : Screen {

    private val W = LevelData.WORLD_WIDTH
    private val H = 800f
    private val camera   = OrthographicCamera()
    private val viewport = FitViewport(W, H, camera)
    private val font      = BitmapFont().also { it.data.setScale(3f) }
    private val smallFont = BitmapFont().also { it.data.setScale(2f) }

    private var time = 0f
    private var showSettings = false

    private var btnStart         = Rectangle()
    private var btnSettings      = Rectangle()
    private var btnSoundToggle   = Rectangle()
    private var btnMusicToggle   = Rectangle()
    private var btnCloseSettings = Rectangle()

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val sh = Gdx.graphics.height.toFloat()
                val fx = screenX.toFloat(); val fy = screenY.toFloat()

                if (showSettings) {
                    if (UIHelper.hits(btnSoundToggle,   fx, fy, sh)) GameSettings.soundEnabled = !GameSettings.soundEnabled
                    if (UIHelper.hits(btnMusicToggle,   fx, fy, sh)) GameSettings.musicEnabled = !GameSettings.musicEnabled
                    if (UIHelper.hits(btnCloseSettings, fx, fy, sh)) showSettings = false
                    return true
                }
                if (UIHelper.hits(btnStart,    fx, fy, sh)) { game.setScreen(GameScreen(game, 1)); dispose() }
                if (UIHelper.hits(btnSettings, fx, fy, sh)) { showSettings = true }
                return true
            }
        }
    }

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(UITheme.BG_DARK.r, UITheme.BG_DARK.g, UITheme.BG_DARK.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val sr = game.shapeRenderer
        sr.projectionMatrix  = camera.combined
        game.batch.projectionMatrix = camera.combined

        drawBackground(sr)
        drawTitle()
        drawButtons(sr)
        if (showSettings) drawSettingsPanel(sr)
    }

    private fun drawBackground(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.15f, 0.15f, 0.17f, 1f)
        sr.rect(W / 2f - 80f, 0f, 160f, H)
        sr.end()

        sr.begin(ShapeRenderer.ShapeType.Filled)
        val offset = (time * 200f) % 100f
        sr.color = Color(0.28f, 0.28f, 0.30f, 1f)
        var y = -offset
        while (y < H) { sr.rect(W / 2f - 4f, y, 8f, 50f); y += 100f }
        sr.end()

        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(UITheme.ACCENT_RED.r, UITheme.ACCENT_RED.g, UITheme.ACCENT_RED.b, 0.18f)
        sr.rect(W / 2f - 22f, H * 0.30f, 44f, 80f)
        sr.end()
    }

    private fun drawTitle() {
        game.batch.begin()
        if (showWin) {
            UIHelper.drawCentredText(game.batch, font, "ALL LEVELS", W / 2f, H * 0.82f, UITheme.ACCENT_GREEN)
            UIHelper.drawCentredText(game.batch, font, "COMPLETE!",  W / 2f, H * 0.75f, UITheme.ACCENT)
        } else {
            UIHelper.drawCentredText(game.batch, font, "DRIFT", W / 2f, H * 0.82f, UITheme.ACCENT)
            UIHelper.drawCentredText(game.batch, font, "PARK",  W / 2f, H * 0.74f, UITheme.TEXT_WHITE)
        }
        val pulse = 0.5f + 0.5f * Math.sin(time * 2.5).toFloat()
        UIHelper.drawCentredText(game.batch, smallFont, "Tap to Start",
            W / 2f, H * 0.22f, Color(UITheme.TEXT_DIM.r, UITheme.TEXT_DIM.g, UITheme.TEXT_DIM.b, pulse))
        game.batch.end()
    }

    private fun drawButtons(sr: ShapeRenderer) {
        val bw = 260f; val bh = 70f; val bx = W / 2f - bw / 2f
        btnStart = UIHelper.drawButton(sr, game.batch, smallFont,
            if (showWin) "PLAY AGAIN" else "START GAME",
            bx, H * 0.42f, bw, bh, UITheme.BTN_PRIMARY, UITheme.BG_DARK)
        btnSettings = UIHelper.drawButton(sr, game.batch, smallFont,
            "SETTINGS", bx, H * 0.42f - bh - 20f, bw, bh, UITheme.BTN_SECONDARY, UITheme.TEXT_WHITE)
    }

    private fun drawSettingsPanel(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = UITheme.OVERLAY
        sr.rect(0f, 0f, W, H)
        sr.end()

        val pw = 340f; val ph = 380f
        val px = W / 2f - pw / 2f; val py = H / 2f - ph / 2f
        UIHelper.drawPanel(sr, px, py, pw, ph, UITheme.BG_PANEL)

        game.batch.begin()
        UIHelper.drawCentredText(game.batch, font, "SETTINGS", W / 2f, py + ph - 30f, UITheme.ACCENT)
        game.batch.end()

        val bw = 260f; val bh = 65f; val bx = W / 2f - bw / 2f

        val soundLabel = if (GameSettings.soundEnabled) "SOUND: ON" else "SOUND: OFF"
        val soundCol   = if (GameSettings.soundEnabled) UITheme.ACCENT_GREEN else UITheme.ACCENT_RED
        btnSoundToggle = UIHelper.drawButton(sr, game.batch, smallFont,
            soundLabel, bx, py + ph - 140f, bw, bh, soundCol, UITheme.BG_DARK)

        val musicLabel = if (GameSettings.musicEnabled) "MUSIC: ON" else "MUSIC: OFF"
        val musicCol   = if (GameSettings.musicEnabled) UITheme.ACCENT_GREEN else UITheme.ACCENT_RED
        btnMusicToggle = UIHelper.drawButton(sr, game.batch, smallFont,
            musicLabel, bx, py + ph - 230f, bw, bh, musicCol, UITheme.BG_DARK)

        btnCloseSettings = UIHelper.drawButton(sr, game.batch, smallFont,
            "CLOSE", bx, py + 30f, bw, bh, UITheme.BTN_SECONDARY, UITheme.TEXT_WHITE)
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
    override fun hide()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun dispose() { font.dispose(); smallFont.dispose() }
}