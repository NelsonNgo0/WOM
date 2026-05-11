package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport

class GameScreen(private val game: ParkingGame, private val levelNumber: Int) : Screen {

    private val viewW = 480f
    private val viewH = 800f
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(viewW, viewH, camera)

    private val level = LevelData.get(levelNumber)
    private val car = Car(level.carStartX, level.carStartY, level.carStartAngle)
    private val parkingSpot = level.parkingSpot
    private val input = TouchInputHandler()

    private val font      = BitmapFont().also { it.data.setScale(2.8f) }
    private val smallFont = BitmapFont().also { it.data.setScale(2f) }
    private val layout    = GlyphLayout()

    // --- State ---
    private enum class State { PLAYING, PARKED, CRASHED, PAUSED }
    private var state = State.PLAYING
    private var stateTimer = 0f
    private var shakeAmount = 0f
    private var stallTimer = 0f

    // --- Pause ---
    private var isPaused = false
    private var btnPause        = Rectangle()
    private var btnResume       = Rectangle()
    private var btnPauseHome    = Rectangle()
    private var btnSoundToggle  = Rectangle()
    private var btnMusicToggle  = Rectangle()

    // --- Tyre marks ---
    data class TyreMark(val x: Float, val y: Float, val angle: Float, val alpha: Float)
    private val tyreMarks = ArrayDeque<TyreMark>(200)
    private var markTimer = 0f

    private var showArrow = false

    companion object {
        const val MARK_INTERVAL   = 0.04f
        const val MAX_MARKS       = 200
        const val ARROW_SHOW_DIST = 350f
    }

    // -------------------------------------------------------------------------
    override fun show() {
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(object : com.badlogic.gdx.InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val sh = Gdx.graphics.height.toFloat()
                val fx = screenX.toFloat()
                val fy = screenY.toFloat()

                if (isPaused) {
                    if (UIHelper.hits(btnResume,      fx, fy, sh)) { isPaused = false }
                    if (UIHelper.hits(btnPauseHome,   fx, fy, sh)) { game.setScreen(MenuScreen(game)); dispose() }
                    if (UIHelper.hits(btnSoundToggle, fx, fy, sh)) { GameSettings.soundEnabled = !GameSettings.soundEnabled }
                    if (UIHelper.hits(btnMusicToggle, fx, fy, sh)) { GameSettings.musicEnabled = !GameSettings.musicEnabled }
                    return true
                }

                if (UIHelper.hits(btnPause, fx, fy, sh)) {
                    isPaused = true
                    return true
                }
                return false
            }
        })
        multiplexer.addProcessor(input)
        Gdx.input.inputProcessor = multiplexer
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }

    // -------------------------------------------------------------------------
    override fun render(delta: Float) {
        val dt = minOf(delta, 0.05f)
        if (!isPaused) update(dt)
        draw()
    }

    private fun update(delta: Float) {
        when (state) {
            State.PLAYING -> updatePlaying(delta)
            State.PARKED  -> { stateTimer += delta; if (stateTimer >= 1.2f) advanceLevel() }
            State.CRASHED -> {}
            State.PAUSED  -> {}
        }
    }

    private fun updatePlaying(delta: Float) {
        car.update(delta, input.isBraking, input.steerValue)

        // Tyre marks
        markTimer += delta
        if (car.isDrifting && markTimer >= MARK_INTERVAL) {
            markTimer = 0f
            if (tyreMarks.size >= MAX_MARKS) tyreMarks.removeFirst()
            tyreMarks.addLast(TyreMark(car.position.x, car.position.y, car.angle, 1f))
        }

        // Stall detection
        if (car.speed < 5f) {
            stallTimer += delta
            if (stallTimer > 3f) onCrash()
        } else {
            stallTimer = 0f
        }

        // Collisions
        for (obs in level.obstacles) {
            if (car.collidesWith(obs.bounds)) { onCrash(); return }
        }

        // Off world
        if (car.position.y > level.worldHeight + 200f || car.position.y < -200f ||
            car.position.x < -200f || car.position.x > LevelData.WORLD_WIDTH + 200f) {
            onCrash(); return
        }

        // Parking
        if (car.isInsideSpot(parkingSpot)) { onParked() }

        // Arrow hint
        val spotCX = parkingSpot.bounds.x + parkingSpot.bounds.width / 2f
        val spotCY = parkingSpot.bounds.y + parkingSpot.bounds.height / 2f
        showArrow = Vector2.dst(car.position.x, car.position.y, spotCX, spotCY) < ARROW_SHOW_DIST
    }

    private fun onCrash()  { game.setScreen(LoseScreen(game, levelNumber)); dispose() }
    private fun onParked() { state = State.PARKED; stateTimer = 0f; parkingSpot.isOccupied = true; car.isParked = true }
    private fun advanceLevel() {
        if (levelNumber < 5) game.setScreen(LevelClearScreen(game, levelNumber))
        else game.setScreen(WinScreen(game))
        dispose()
    }

    // -------------------------------------------------------------------------
    // Draw
    // -------------------------------------------------------------------------
    private fun draw() {
        val rc = Color(0.13f, 0.13f, 0.15f, 1f)
        Gdx.gl.glClearColor(rc.r, rc.g, rc.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val camY = (car.position.y + shakeOffset())
            .coerceIn(viewH / 2f, level.worldHeight - viewH / 2f)
        camera.position.set(LevelData.WORLD_WIDTH / 2f, camY, 0f)
        camera.update()

        val sr = game.shapeRenderer
        sr.projectionMatrix = camera.combined

        drawRoad(sr)
        drawTyreMarks(sr)
        drawParkingSpot(sr)
        drawObstacles(sr)
        drawCar(sr)
        if (showArrow) drawArrow(sr)

        drawHUD()
        drawPauseButton()
        if (isPaused) drawPausePanel()
        if (state == State.PARKED) drawParkedOverlay()
    }

    private fun shakeOffset() =
        if (shakeAmount > 0f) MathUtils.random(-shakeAmount, shakeAmount) else 0f

    private fun drawRoad(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.18f, 0.18f, 0.20f, 1f)
        sr.rect(LevelData.ROAD_LEFT, 0f, LevelData.ROAD_RIGHT - LevelData.ROAD_LEFT, level.worldHeight)
        val tY = parkingSpot.bounds.y - 60f
        sr.rect(0f, tY, LevelData.WORLD_WIDTH, parkingSpot.bounds.height + 120f)
        sr.end()

        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.32f, 0.32f, 0.32f, 1f)
        var y = 0f
        while (y < level.worldHeight) {
            sr.rect(LevelData.ROAD_CENTRE - 3f, y, 6f, 28f); y += 56f
        }
        sr.end()
    }

    private fun drawTyreMarks(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        tyreMarks.forEachIndexed { i, mark ->
            val alpha = (i.toFloat() / tyreMarks.size) * 0.5f
            sr.color = Color(0.05f, 0.05f, 0.05f, alpha)
            val rad = (mark.angle - 90f) * MathUtils.degreesToRadians
            val px = MathUtils.cos(rad + MathUtils.PI / 2f) * 10f
            val py = MathUtils.sin(rad + MathUtils.PI / 2f) * 10f
            sr.ellipse(mark.x + px - 4f, mark.y + py - 4f, 8f, 8f)
            sr.ellipse(mark.x - px - 4f, mark.y - py - 4f, 8f, 8f)
        }
        sr.end()
    }

    private fun drawParkingSpot(sr: ShapeRenderer) {
        val sb = parkingSpot.bounds
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = if (parkingSpot.isOccupied) Color(0.2f, 0.9f, 0.3f, 0.5f)
        else Color(0.95f, 0.85f, 0.1f, 0.3f)
        sr.rect(sb.x, sb.y, sb.width, sb.height)
        sr.end()
        sr.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(3f)
        sr.color = if (parkingSpot.isOccupied) Color.GREEN else Color.YELLOW
        sr.rect(sb.x, sb.y, sb.width, sb.height)
        sr.end()
    }

    private fun drawObstacles(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        for (obs in level.obstacles) {
            sr.color = obs.color
            sr.rect(obs.bounds.x, obs.bounds.y, obs.bounds.width, obs.bounds.height)
        }
        sr.end()
    }

    private fun drawCar(sr: ShapeRenderer) {
        val cx = car.position.x; val cy = car.position.y
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.identity(); sr.translate(cx, cy, 0f); sr.rotate(0f, 0f, 1f, car.angle - 90f)
        sr.color = Color(0f, 0f, 0f, 0.3f)
        sr.rect(-car.width / 2f + 3f, -car.height / 2f - 3f, car.width, car.height)
        val driftHeat = if (car.isDrifting) 0.3f else 0f
        sr.color = Color(0.85f + driftHeat, 0.2f, 0.2f, 1f)
        sr.rect(-car.width / 2f, -car.height / 2f, car.width, car.height)
        sr.color = Color(0.6f, 0.85f, 1f, 0.85f)
        sr.rect(-car.width / 2f + 4f, car.height / 2f - 17f, car.width - 8f, 13f)
        sr.color = Color(0.5f, 0.75f, 0.9f, 0.7f)
        sr.rect(-car.width / 2f + 4f, -car.height / 2f + 4f, car.width - 8f, 10f)
        sr.color = Color(0.08f, 0.08f, 0.08f, 1f)
        val wx = 7f; val wh = 13f
        sr.rect(-car.width/2f-4f, car.height/2f-wh-5f, wx, wh)
        sr.rect(car.width/2f-3f,  car.height/2f-wh-5f, wx, wh)
        sr.rect(-car.width/2f-4f, -car.height/2f+5f,   wx, wh)
        sr.rect(car.width/2f-3f,  -car.height/2f+5f,   wx, wh)
        if (car.isBraking) {
            sr.color = Color(1f, 0.1f, 0.1f, 0.9f)
            sr.rect(-car.width/2f+2f,  -car.height/2f, 8f, 5f)
            sr.rect(car.width/2f-10f, -car.height/2f, 8f, 5f)
        }
        sr.identity(); sr.end()
    }

    private fun drawArrow(sr: ShapeRenderer) {
        val spotCX = parkingSpot.bounds.x + parkingSpot.bounds.width / 2f
        val spotCY = parkingSpot.bounds.y + parkingSpot.bounds.height / 2f
        val arrowAngle = MathUtils.atan2(spotCY - car.position.y, spotCX - car.position.x) * MathUtils.radiansToDegrees
        val arrowX = car.position.x + MathUtils.cosDeg(arrowAngle) * 80f
        val arrowY = car.position.y + MathUtils.sinDeg(arrowAngle) * 80f
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(1f, 1f, 0.2f, 0.85f)
        sr.identity(); sr.translate(arrowX, arrowY, 0f); sr.rotate(0f, 0f, 1f, arrowAngle - 90f)
        sr.triangle(0f, 18f, -12f, -10f, 12f, -10f)
        sr.identity(); sr.end()
    }

    // -------------------------------------------------------------------------
    // HUD (screen space)
    // -------------------------------------------------------------------------
    private fun hudCamera(): OrthographicCamera {
        val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
        val cam = OrthographicCamera(sw, sh)
        cam.position.set(sw / 2f, sh / 2f, 0f); cam.update()
        return cam
    }

    private fun drawHUD() {
        val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
        val hc = hudCamera()
        val sr = game.shapeRenderer
        sr.projectionMatrix = hc.combined

        // Joystick
        val anchor = input.getJoystickAnchor(); val knob = input.getJoystickKnob()
        if (anchor != null && knob != null) {
            sr.begin(ShapeRenderer.ShapeType.Line)
            Gdx.gl.glLineWidth(2f)
            sr.color = Color(1f, 1f, 1f, 0.35f)
            sr.circle(anchor.x, sh - anchor.y, 80f, 32); sr.end()
            sr.begin(ShapeRenderer.ShapeType.Filled)
            sr.color = Color(1f, 1f, 1f, 0.65f)
            sr.circle(knob.x, sh - knob.y, 26f, 24); sr.end()
        } else {
            sr.begin(ShapeRenderer.ShapeType.Line)
            sr.color = Color(1f, 1f, 1f, 0.12f)
            sr.circle(sw * 0.75f, sh * 0.28f, 60f, 32); sr.end()
        }

        // Drift bar
        if (car.isDrifting) {
            sr.begin(ShapeRenderer.ShapeType.Filled)
            sr.color = Color(1f, 0.6f, 0.1f, 0.9f)
            sr.rect(sw / 2f - 60f, sh - 40f, 120f, 14f); sr.end()
        }

        game.batch.projectionMatrix = hc.combined
        game.batch.begin()
        font.color = UITheme.TEXT_WHITE
        font.draw(game.batch, level.levelName, 16f, sh - 16f)
        val kmh = (car.speed * 0.05f).toInt()
        font.draw(game.batch, "$kmh km/h", sw - 180f, sh - 16f)
        if (car.isDrifting) {
            font.color = Color(1f, 0.7f, 0.1f, 1f)
            font.draw(game.batch, "DRIFT!", sw / 2f - 30f, sh - 50f)
        }
        font.color = Color(0.5f, 0.5f, 0.5f, 0.4f)
        font.draw(game.batch, "BRAKE", 16f, sh / 2f + 8f)
        font.color = Color.WHITE
        game.batch.end()
    }

    private fun drawPauseButton() {
        val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
        val hc = hudCamera()
        val bw = 80f; val bh = 60f
        btnPause = UIHelper.drawButton(game.shapeRenderer, game.batch, smallFont,
            "II", sw - bw - 10f, sh - bh - 10f, bw, bh,
            UITheme.BTN_SECONDARY, UITheme.TEXT_WHITE)
        game.shapeRenderer.projectionMatrix = hc.combined
        game.batch.projectionMatrix = hc.combined
    }

    private fun drawPausePanel() {
        val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
        val hc = hudCamera()
        val sr = game.shapeRenderer
        sr.projectionMatrix = hc.combined
        game.batch.projectionMatrix = hc.combined

        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = UITheme.OVERLAY
        sr.rect(0f, 0f, sw, sh); sr.end()

        val pw = 320f; val ph = 420f
        val px = sw / 2f - pw / 2f; val py = sh / 2f - ph / 2f
        UIHelper.drawPanel(sr, px, py, pw, ph, UITheme.BG_PANEL)

        UIHelper.drawCentredText(game.batch, font, "PAUSED", sw / 2f, py + ph - 28f, UITheme.ACCENT)

        val bw = 240f; val bh = 65f; val bx = sw / 2f - bw / 2f

        val soundLbl = if (GameSettings.soundEnabled) "SOUND: ON" else "SOUND: OFF"
        val soundCol = if (GameSettings.soundEnabled) UITheme.ACCENT_GREEN else UITheme.ACCENT_RED
        btnSoundToggle = UIHelper.drawButton(sr, game.batch, smallFont,
            soundLbl, bx, py + ph - 165f, bw, bh, soundCol, UITheme.BG_DARK)

        val musicLbl = if (GameSettings.musicEnabled) "MUSIC: ON" else "MUSIC: OFF"
        val musicCol = if (GameSettings.musicEnabled) UITheme.ACCENT_GREEN else UITheme.ACCENT_RED
        btnMusicToggle = UIHelper.drawButton(sr, game.batch, smallFont,
            musicLbl, bx, py + ph - 250f, bw, bh, musicCol, UITheme.BG_DARK)

        btnResume = UIHelper.drawButton(sr, game.batch, smallFont,
            "RESUME", bx, py + 105f, bw, bh, UITheme.ACCENT, UITheme.BG_DARK)

        btnPauseHome = UIHelper.drawButton(sr, game.batch, smallFont,
            "HOME", bx, py + 25f, bw, bh, UITheme.BTN_SECONDARY, UITheme.TEXT_WHITE)
    }

    private fun drawParkedOverlay() {
        val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
        val hc = hudCamera()
        val sr = game.shapeRenderer
        sr.projectionMatrix = hc.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0f, 0f, 0f, 0.45f)
        sr.rect(0f, 0f, sw, sh); sr.end()
        game.batch.projectionMatrix = hc.combined
        game.batch.begin()
        font.color = UITheme.ACCENT_GREEN
        layout.setText(font, "PERFECT PARK!")
        font.draw(game.batch, "PERFECT PARK!", (sw - layout.width) / 2f, sh / 2f + 10f)
        font.color = Color.WHITE
        game.batch.end()
    }

    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { font.dispose(); smallFont.dispose() }
}