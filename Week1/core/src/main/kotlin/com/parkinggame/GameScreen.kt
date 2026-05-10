package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * GameScreen.kt
 *
 * Key changes:
 * - Camera follows the car (scrolling world)
 * - Drift smoke/tyre marks when drifting
 * - Perpendicular parking spot at T-intersection
 * - Arrow hint points toward parking spot when car is near
 */
class GameScreen(private val game: ParkingGame, private val levelNumber: Int) : Screen {

    // Viewport: what the player sees at once (portrait phone window)
    private val viewW = 480f
    private val viewH = 800f

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(viewW, viewH, camera)

    private val level = LevelData.get(levelNumber)
    private val car = Car(level.carStartX, level.carStartY, level.carStartAngle)
    private val parkingSpot = level.parkingSpot

    private val input = TouchInputHandler()
    private val font = BitmapFont().also { it.data.setScale(2.8f) }
    private val layout = GlyphLayout()

    private enum class State { PLAYING, PARKED, CRASHED }
    private var state = State.PLAYING
    private var stateTimer = 0f
    private var shakeAmount = 0f

    private var stallTimer = 0f

    // Tyre marks — stored as world positions
    data class TyreMark(val x: Float, val y: Float, val angle: Float, val alpha: Float)
    private val tyreMarks = ArrayDeque<TyreMark>(200)
    private var markTimer = 0f

    // Arrow hint visibility
    private var showArrow = false

    companion object {
        const val NEXT_LEVEL_DELAY = 2.0f
        const val RESTART_DELAY = 1.5f
        const val MARK_INTERVAL = 0.04f
        const val MAX_MARKS = 200
        // Show arrow when car is within this many units of the T-intersection
        const val ARROW_SHOW_DIST = 350f
    }

    override fun show() {
        Gdx.input.inputProcessor = input
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    // -------------------------------------------------------------------------
    // Game loop
    // -------------------------------------------------------------------------

    override fun render(delta: Float) {
        val dt = minOf(delta, 0.05f)
        update(dt)
        draw()
    }

    private fun update(delta: Float) {
        when (state) {
            State.PLAYING  -> updatePlaying(delta)
            State.PARKED   -> { stateTimer += delta; if (stateTimer >= NEXT_LEVEL_DELAY) advanceLevel() }
            State.CRASHED  -> { stateTimer += delta; shakeAmount = maxOf(0f, shakeAmount - delta * 8f)
                if (stateTimer >= RESTART_DELAY) restartLevel() }
        }
    }

    private fun updatePlaying(delta: Float) {
        car.update(delta, input.isBraking, input.steerValue)

        // Tyre marks when drifting
        markTimer += delta
        if (car.isDrifting && markTimer >= MARK_INTERVAL) {
            markTimer = 0f
            if (tyreMarks.size >= MAX_MARKS) tyreMarks.removeFirst()
            tyreMarks.addLast(TyreMark(car.position.x, car.position.y, car.angle, 1f))
        }

        // stall timer
        if (car.speed < 5f) {
            stallTimer  += delta
            if (stallTimer > 3f) onCrash()
        } else {
            stallTimer = 0f
        }

        // Collision with obstacles
        for (obs in level.obstacles) {
            if (car.collidesWith(obs.bounds)) { onCrash(); return }
        }

        // Off world
        if (car.position.y > level.worldHeight + 200f || car.position.y < -200f ||
            car.position.x < -200f || car.position.x > LevelData.WORLD_WIDTH + 200f) {
            onCrash(); return
        }

        // Parking check
        if (car.isInsideSpot(parkingSpot)) { onParked() }

        // Show arrow when near the T-intersection
        val distToSpot = Vector2.dst(car.position.x, car.position.y,
            parkingSpot.bounds.x + parkingSpot.bounds.width / 2f,
            parkingSpot.bounds.y + parkingSpot.bounds.height / 2f)
        showArrow = distToSpot < ARROW_SHOW_DIST
    }

    private fun onCrash() {
        state = State.CRASHED; stateTimer = 0f; shakeAmount = 10f
    }

    private fun onParked() {
        state = State.PARKED; stateTimer = 0f
        parkingSpot.isOccupied = true; car.isParked = true
    }

    private fun advanceLevel() {
        if (levelNumber < 2) game.setScreen(GameScreen(game, levelNumber + 1))
        else game.setScreen(MenuScreen(game, showWin = true))
        dispose()
    }

    private fun restartLevel() {
        game.setScreen(GameScreen(game, levelNumber)); dispose()
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private fun draw() {
        val rc = Color(0.13f, 0.13f, 0.15f, 1f)
        Gdx.gl.glClearColor(rc.r, rc.g, rc.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // === Camera follows car, clamped so we don't show outside the world ===
        val camX = LevelData.WORLD_WIDTH / 2f   // Always centred horizontally
        val camY = (car.position.y + shakeOffset()).coerceIn(viewH / 2f, level.worldHeight - viewH / 2f)
        camera.position.set(camX, camY, 0f)
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
        if (state != State.PLAYING) drawStateOverlay()
    }

    private fun shakeOffset(): Float =
        if (shakeAmount > 0f) MathUtils.random(-shakeAmount, shakeAmount) else 0f

    private fun drawRoad(sr: ShapeRenderer) {
        // Road surface (slightly lighter than background)
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.18f, 0.18f, 0.20f, 1f)
        sr.rect(LevelData.ROAD_LEFT, 0f,
            LevelData.ROAD_RIGHT - LevelData.ROAD_LEFT, level.worldHeight)

        // T-intersection horizontal road
        val tY = parkingSpot.bounds.y - 60f
        sr.rect(0f, tY, LevelData.WORLD_WIDTH, parkingSpot.bounds.height + 120f)
        sr.end()

        // Dashed centre line
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.35f, 0.35f, 0.35f, 1f)
        var y = 0f
        while (y < level.worldHeight) {
            sr.rect(LevelData.ROAD_CENTRE - 3f, y, 6f, 28f)
            y += 56f
        }
        sr.end()
    }

    private fun drawTyreMarks(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        tyreMarks.forEachIndexed { i, mark ->
            val alpha = (i.toFloat() / tyreMarks.size) * 0.5f
            sr.color = Color(0.05f, 0.05f, 0.05f, alpha)
            // Two tyre tracks
            val rad = (mark.angle - 90f) * MathUtils.degreesToRadians
            val perpX = MathUtils.cos(rad + MathUtils.PI / 2f) * 10f
            val perpY = MathUtils.sin(rad + MathUtils.PI / 2f) * 10f
            sr.ellipse(mark.x + perpX - 4f, mark.y + perpY - 4f, 8f, 8f)
            sr.ellipse(mark.x - perpX - 4f, mark.y - perpY - 4f, 8f, 8f)
        }
        sr.end()
    }

    private fun drawParkingSpot(sr: ShapeRenderer) {
        val sb = parkingSpot.bounds
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = if (parkingSpot.isOccupied)
            Color(0.2f, 0.9f, 0.3f, 0.5f) else Color(0.95f, 0.85f, 0.1f, 0.3f)
        sr.rect(sb.x, sb.y, sb.width, sb.height)
        sr.end()

        sr.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(3f)
        sr.color = if (parkingSpot.isOccupied) Color.GREEN else Color.YELLOW
        sr.rect(sb.x, sb.y, sb.width, sb.height)
        // P marking lines
        sr.line(sb.x, sb.y, sb.x, sb.y + sb.height)
        sr.line(sb.x + sb.width, sb.y, sb.x + sb.width, sb.y + sb.height)
        sr.end()
    }

    private fun drawObstacles(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        for (obs in level.obstacles) {
            sr.color = obs.color
            sr.rect(obs.bounds.x, obs.bounds.y, obs.bounds.width, obs.bounds.height)
        }
        sr.end()

        // Kerb stripe detail on walls
        sr.begin(ShapeRenderer.ShapeType.Filled)
        for (obs in level.obstacles) {
            if (obs.bounds.width > 40f) {  // Only on thick walls
                var stripeY = obs.bounds.y
                while (stripeY < obs.bounds.y + obs.bounds.height) {
                    sr.color = if (((stripeY / 30f).toInt() % 2) == 0)
                        Color(0.9f, 0.9f, 0.9f, 0.15f) else Color(1f, 0.2f, 0.1f, 0.15f)
                    sr.rect(obs.bounds.x, stripeY,
                        minOf(obs.bounds.width, 20f), minOf(30f, obs.bounds.height))
                    stripeY += 30f
                }
            }
        }
        sr.end()
    }

    private fun drawCar(sr: ShapeRenderer) {
        val cx = car.position.x
        val cy = car.position.y

        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.identity()
        sr.translate(cx, cy, 0f)
        sr.rotate(0f, 0f, 1f, car.angle - 90f)

        // Shadow
        sr.color = Color(0f, 0f, 0f, 0.3f)
        sr.rect(-car.width / 2f + 3f, -car.height / 2f - 3f, car.width, car.height)

        // Body — colour shifts slightly red when drifting
        val driftHeat = if (car.isDrifting) 0.3f else 0f
        sr.color = Color(0.85f + driftHeat, 0.2f, 0.2f, 1f)
        sr.rect(-car.width / 2f, -car.height / 2f, car.width, car.height)

        // Windscreen
        sr.color = Color(0.6f, 0.85f, 1f, 0.85f)
        sr.rect(-car.width / 2f + 4f, car.height / 2f - 17f, car.width - 8f, 13f)

        // Rear window
        sr.color = Color(0.5f, 0.75f, 0.9f, 0.7f)
        sr.rect(-car.width / 2f + 4f, -car.height / 2f + 4f, car.width - 8f, 10f)

        // Wheels
        sr.color = Color(0.08f, 0.08f, 0.08f, 1f)
        val wx = 7f; val wh = 13f
        sr.rect(-car.width / 2f - 4f,  car.height / 2f - wh - 5f, wx, wh)
        sr.rect( car.width / 2f - 3f,  car.height / 2f - wh - 5f, wx, wh)
        sr.rect(-car.width / 2f - 4f, -car.height / 2f + 5f, wx, wh)
        sr.rect( car.width / 2f - 3f, -car.height / 2f + 5f, wx, wh)

        // Brake lights — bright when braking
        if (car.isBraking) {
            sr.color = Color(1f, 0.1f, 0.1f, 0.9f)
            sr.rect(-car.width / 2f + 2f, -car.height / 2f, 8f, 5f)
            sr.rect( car.width / 2f - 10f, -car.height / 2f, 8f, 5f)
        }

        sr.identity()
        sr.end()
    }

    /**
     * Draws a directional arrow pointing toward the parking spot.
     * Appears when the car is close to the T-intersection.
     */
    private fun drawArrow(sr: ShapeRenderer) {
        val spotCX = parkingSpot.bounds.x + parkingSpot.bounds.width / 2f
        val spotCY = parkingSpot.bounds.y + parkingSpot.bounds.height / 2f
        val dx = spotCX - car.position.x
        val dy = spotCY - car.position.y
        val arrowAngle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees

        // Draw arrow 60px ahead of car
        val arrowX = car.position.x + MathUtils.cosDeg(arrowAngle) * 80f
        val arrowY = car.position.y + MathUtils.sinDeg(arrowAngle) * 80f

        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(1f, 1f, 0.2f, 0.85f)
        sr.identity()
        sr.translate(arrowX, arrowY, 0f)
        sr.rotate(0f, 0f, 1f, arrowAngle - 90f)
        // Arrow triangle
        sr.triangle(0f, 18f, -12f, -10f, 12f, -10f)
        sr.identity()
        sr.end()
    }

    // -------------------------------------------------------------------------
    // HUD — drawn in screen coordinates
    // -------------------------------------------------------------------------

    private fun drawHUD() {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val hudCam = OrthographicCamera(sw, sh)
        hudCam.position.set(sw / 2f, sh / 2f, 0f)
        hudCam.update()

        val sr = game.shapeRenderer
        sr.projectionMatrix = hudCam.combined

//        // Brake zone
//        sr.begin(ShapeRenderer.ShapeType.Filled)
//        val brakeAlpha = if (input.isBrakeTouched()) 0.18f else 0f
//        sr.color = Color(1f, 0.3f, 0.3f, brakeAlpha)
//        sr.rect(0f, 0f, sw / 2f, sh)
//        sr.end()

        // Joystick
        val anchor = input.getJoystickAnchor()
        val knob   = input.getJoystickKnob()
        if (anchor != null && knob != null) {
            sr.begin(ShapeRenderer.ShapeType.Line)
            Gdx.gl.glLineWidth(2f)
            sr.color = Color(1f, 1f, 1f, 0.35f)
            sr.circle(anchor.x, sh - anchor.y, 80f, 32)
            sr.end()
            sr.begin(ShapeRenderer.ShapeType.Filled)
            sr.color = Color(1f, 1f, 1f, 0.65f)
            sr.circle(knob.x, sh - knob.y, 26f, 24)
            sr.end()
        } else {
            sr.begin(ShapeRenderer.ShapeType.Line)
            sr.color = Color(1f, 1f, 1f, 0.12f)
            sr.circle(sw * 0.75f, sh * 0.28f, 60f, 32)
            sr.end()
        }

        // Drift indicator bar
        if (car.isDrifting) {
            sr.begin(ShapeRenderer.ShapeType.Filled)
            sr.color = Color(1f, 0.6f, 0.1f, 0.9f)
            sr.rect(sw / 2f - 60f, sh - 30f, 120f, 14f)
            sr.end()
        }

        // Text
        game.batch.projectionMatrix = hudCam.combined
        game.batch.begin()
        font.color = Color(1f, 1f, 1f, 0.8f)
        font.draw(game.batch, level.levelName, 16f, sh - 16f)

        val speedKmh = (car.speed * 0.05f).toInt()
        font.draw(game.batch, "${speedKmh} km/h", sw - 100f, sh - 16f)

        if (car.isDrifting) {
            font.color = Color(1f, 0.7f, 0.1f, 1f)
            font.draw(game.batch, "DRIFT!", sw / 2f - 22f, sh - 38f)
        }

        font.color = Color(0.6f, 0.6f, 0.6f, 0.5f)
        font.draw(game.batch, "BRAKE", 16f, sh / 2f + 8f)
        font.color = Color.WHITE
        game.batch.end()
    }

    private fun drawStateOverlay() {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val hudCam = OrthographicCamera(sw, sh)
        hudCam.position.set(sw / 2f, sh / 2f, 0f)
        hudCam.update()

        val sr = game.shapeRenderer
        sr.projectionMatrix = hudCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0f, 0f, 0f, 0.55f)
        sr.rect(0f, 0f, sw, sh)
        sr.end()

        game.batch.projectionMatrix = hudCam.combined
        game.batch.begin()
        when (state) {
            State.PARKED -> {
                font.color = Color.GREEN
                val msg = if (levelNumber < 2) "PERFECT PARK! ✓" else "YOU WIN! 🏆"
                layout.setText(font, msg)
                font.draw(game.batch, msg, (sw - layout.width) / 2f, sh / 2f + 10f)
            }
            State.CRASHED -> {
                font.color = Color(1f, 0.3f, 0.3f, 1f)
                val msg = "CRASHED!  Restarting..."
                layout.setText(font, msg)
                font.draw(game.batch, msg, (sw - layout.width) / 2f, sh / 2f + 10f)
            }
            else -> {}
        }
        font.color = Color.WHITE
        game.batch.end()
    }

    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { font.dispose() }
}