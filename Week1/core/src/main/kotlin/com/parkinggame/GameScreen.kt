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
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * GameScreen.kt
 * Path: core/src/main/kotlin/com/parkinggame/GameScreen.kt
 *
 * The main gameplay screen. Handles:
 *  - Game loop (update + render)
 *  - Camera & viewport (virtual 480×800 world mapped to any screen size)
 *  - Collision detection
 *  - Win/lose conditions
 *  - HUD drawing (brake zone, joystick, level name, speed)
 */
class GameScreen(private val game: ParkingGame, private val levelNumber: Int) : Screen {

    // --- Virtual world size (all game logic uses these coordinates) ---
    private val worldWidth = LevelData.WORLD_WIDTH
    private val worldHeight = LevelData.WORLD_HEIGHT

    // --- Camera: renders the game world ---
    // FitViewport keeps aspect ratio by adding black bars if needed
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(worldWidth, worldHeight, camera)

    // --- Level data ---
    private val level = LevelData.get(levelNumber)
    private val car = Car(level.carStartX, level.carStartY, level.carStartAngle)
    private val parkingSpot = level.parkingSpot

    // --- Input ---
    private val input = TouchInputHandler()

    // --- Font for HUD text ---
    private val font = BitmapFont()   // Default LibGDX font (white, ~15px)
    private val layout = GlyphLayout()

    // --- Game state ---
    private enum class State { PLAYING, PARKED, CRASHED }
    private var state = State.PLAYING
    private var stateTimer = 0f         // Seconds in current state (used for transition delays)

    // Trail effect: store last N car positions for a skid-mark feel
    data class TrailPoint(val x: Float, val y: Float, val alpha: Float)
    private val trail = ArrayDeque<TrailPoint>(MAX_TRAIL)
    private var trailTimer = 0f

    // Camera shake on crash
    private var shakeAmount = 0f

    companion object {
        const val MAX_TRAIL = 40
        const val TRAIL_INTERVAL = 0.05f    // Seconds between trail points
        const val NEXT_LEVEL_DELAY = 2.0f   // Seconds before advancing after park
        const val RESTART_DELAY = 1.5f      // Seconds before restarting after crash
    }

    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    override fun show() {
        // Register our input handler with LibGDX
        Gdx.input.inputProcessor = input
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    // -------------------------------------------------------------------------
    // Main game loop — called every frame by LibGDX
    // -------------------------------------------------------------------------

    override fun render(delta: Float) {
        // Clamp delta to avoid huge jumps if the app pauses
        val dt = minOf(delta, 0.05f)

        update(dt)
        draw()
    }

    private fun update(delta: Float) {
        when (state) {
            State.PLAYING -> updatePlaying(delta)
            State.PARKED  -> updateParked(delta)
            State.CRASHED -> updateCrashed(delta)
        }
    }

    private fun updatePlaying(delta: Float) {
        // Update car physics
        car.update(delta, input.isBraking, input.steerValue)

        // --- Trail ---
        trailTimer += delta
        if (trailTimer >= TRAIL_INTERVAL) {
            trailTimer = 0f
            if (trail.size >= MAX_TRAIL) trail.removeFirst()
            trail.addLast(TrailPoint(car.position.x, car.position.y, 1f))
        }

        // --- Collision detection ---
        for (obstacle in level.obstacles) {
            if (car.collidesWith(obstacle.bounds)) {
                onCrash()
                return
            }
        }

        // --- Check if off world (fell off the map) ---
        if (car.position.y > worldHeight + 100f ||
            car.position.y < -100f ||
            car.position.x < -100f ||
            car.position.x > worldWidth + 100f) {
            onCrash()
            return
        }

        // --- Check parking ---
        if (car.isInsideSpot(parkingSpot)) {
            onParked()
        }
    }

    private fun updateParked(delta: Float) {
        stateTimer += delta
        if (stateTimer >= NEXT_LEVEL_DELAY) {
            advanceLevel()
        }
    }

    private fun updateCrashed(delta: Float) {
        stateTimer += delta
        shakeAmount = maxOf(0f, shakeAmount - delta * 10f)
        if (stateTimer >= RESTART_DELAY) {
            // Restart the current level
            game.setScreen(GameScreen(game, levelNumber))
            dispose()
        }
    }

    // -------------------------------------------------------------------------
    // Events
    // -------------------------------------------------------------------------

    private fun onCrash() {
        state = State.CRASHED
        stateTimer = 0f
        shakeAmount = 8f   // Pixels of camera shake
    }

    private fun onParked() {
        state = State.PARKED
        stateTimer = 0f
        parkingSpot.isOccupied = true
        car.isParked = true
    }

    private fun advanceLevel() {
        if (levelNumber < 5) {
            game.setScreen(GameScreen(game, levelNumber + 1))
        } else {
            // All levels complete — go back to menu (or show a win screen)
            game.setScreen(MenuScreen(game, showWin = true))
        }
        dispose()
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private fun draw() {
        // Clear screen to road color
        val rc = level.roadColor
        Gdx.gl.glClearColor(rc.r, rc.g, rc.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Apply camera shake
        camera.position.set(
            worldWidth / 2f + MathUtils.random(-shakeAmount, shakeAmount),
            worldHeight / 2f + MathUtils.random(-shakeAmount, shakeAmount),
            0f
        )
        camera.update()

        val sr = game.shapeRenderer

        // --- Draw world (shapes in world coordinates) ---
        sr.projectionMatrix = camera.combined

        // 1. Road markings (dashed center line)
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.25f, 0.25f, 0.25f, 1f)
        var y = 0f
        while (y < worldHeight) {
            sr.rect(worldWidth / 2f - 3f, y, 6f, 30f)
            y += 60f
        }
        sr.end()

        // 2. Parking spot (glowing box)
        sr.begin(ShapeRenderer.ShapeType.Filled)
        val spotColor = when {
            parkingSpot.isOccupied -> Color(0.2f, 0.9f, 0.3f, 0.6f)
            else -> Color(0.9f, 0.9f, 0.2f, 0.4f)
        }
        sr.color = spotColor
        sr.rect(parkingSpot.bounds.x, parkingSpot.bounds.y, parkingSpot.bounds.width, parkingSpot.bounds.height)
        sr.end()

        // Parking spot border
        sr.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(3f)
        sr.color = if (parkingSpot.isOccupied) Color.GREEN else Color.YELLOW
        sr.rect(parkingSpot.bounds.x, parkingSpot.bounds.y, parkingSpot.bounds.width, parkingSpot.bounds.height)
        sr.end()

        // 3. Obstacles
        sr.begin(ShapeRenderer.ShapeType.Filled)
        for (obstacle in level.obstacles) {
            sr.color = obstacle.color
            sr.rect(obstacle.bounds.x, obstacle.bounds.y, obstacle.bounds.width, obstacle.bounds.height)
        }
        sr.end()

        // 4. Car trail (skid marks)
        sr.begin(ShapeRenderer.ShapeType.Filled)
        trail.forEachIndexed { i, point ->
            val alpha = (i.toFloat() / trail.size) * 0.4f
            sr.color = Color(0.1f, 0.1f, 0.1f, alpha)
            sr.ellipse(point.x - 8f, point.y - 5f, 16f, 10f)
        }
        sr.end()

        // 5. Car
        drawCar(sr)

        // 6. HUD (drawn in SCREEN coordinates — use a separate camera)
        drawHUD()

        // 7. State overlay messages
        drawStateOverlay()
    }

    /**
     * Draws the car as a rotated rectangle with details.
     * LibGDX's ShapeRenderer can rotate shapes using a transformation matrix.
     */
    private fun drawCar(sr: ShapeRenderer) {
        val cx = car.position.x
        val cy = car.position.y
        val w = car.width
        val h = car.height

        sr.begin(ShapeRenderer.ShapeType.Filled)

        // Save the current transform, apply rotation around car center
        sr.identity()
        sr.translate(cx, cy, 0f)
        sr.rotate(0f, 0f, 1f, car.angle - 90f)  // -90 because 0° in our system = facing right

        // Car body
        sr.color = Color(0.85f, 0.2f, 0.2f, 1f)  // Red car
        sr.rect(-w / 2f, -h / 2f, w, h)

        // Windscreen (lighter rectangle at front)
        sr.color = Color(0.6f, 0.85f, 1f, 0.8f)
        sr.rect(-w / 2f + 4f, h / 2f - 18f, w - 8f, 14f)

        // Wheels (4 dark rectangles at corners)
        sr.color = Color(0.1f, 0.1f, 0.1f, 1f)
        val wx = 6f; val wh = 14f
        sr.rect(-w / 2f - 3f,  h / 2f - wh - 4f, wx, wh)   // front-left
        sr.rect( w / 2f - 3f,  h / 2f - wh - 4f, wx, wh)   // front-right
        sr.rect(-w / 2f - 3f, -h / 2f + 4f, wx, wh)         // rear-left
        sr.rect( w / 2f - 3f, -h / 2f + 4f, wx, wh)         // rear-right

        // Reset transform
        sr.identity()
        sr.end()
    }

    /**
     * Draws the HUD in SCREEN pixels (not world coordinates).
     * Uses a simple ortho camera matching the actual screen size.
     */
    private fun drawHUD() {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val sr = game.shapeRenderer

        // HUD uses its own projection (screen pixels)
        val hudMatrix = camera.combined.cpy()
        // Reset to screen space:
        val hudCam = OrthographicCamera(sw, sh)
        hudCam.position.set(sw / 2f, sh / 2f, 0f)
        hudCam.update()
        sr.projectionMatrix = hudCam.combined

        // --- Brake zone indicator (left half, semi-transparent) ---
        sr.begin(ShapeRenderer.ShapeType.Filled)
        val brakeAlpha = if (input.isBrakeTouched()) 0.25f else 0.08f
        sr.color = Color(1f, 0.3f, 0.3f, brakeAlpha)
        sr.rect(0f, 0f, sw / 2f, sh)
        sr.end()

        // Brake label
        sr.projectionMatrix = hudCam.combined
        game.batch.projectionMatrix = hudCam.combined
        game.batch.begin()
        font.color = Color(1f, 0.5f, 0.5f, 0.7f)
        font.draw(game.batch, "BRAKE", 20f, sh / 2f)
        font.color = Color.WHITE

        // Level name & speed
        val speedText = "SPEED: ${car.speed.toInt()}"
        font.draw(game.batch, level.levelName, 20f, sh - 20f)
        font.draw(game.batch, speedText, sw - 120f, sh - 20f)
        game.batch.end()

        // --- Virtual joystick (right half) ---
        val anchor = input.getJoystickAnchor()
        val knob = input.getJoystickKnob()
        if (anchor != null && knob != null) {
            sr.begin(ShapeRenderer.ShapeType.Line)
            Gdx.gl.glLineWidth(2f)
            // Outer ring
            sr.color = Color(1f, 1f, 1f, 0.4f)
            sr.circle(anchor.x, sh - anchor.y, 80f, 32)  // Flip Y (LibGDX screen = top-left origin)
            // Inner knob
            sr.end()
            sr.begin(ShapeRenderer.ShapeType.Filled)
            sr.color = Color(1f, 1f, 1f, 0.6f)
            sr.circle(knob.x, sh - knob.y, 25f, 24)
            sr.end()
        } else {
            // Show a faint "touch here" hint
            sr.begin(ShapeRenderer.ShapeType.Line)
            sr.color = Color(1f, 1f, 1f, 0.15f)
            sr.circle(sw * 0.75f, sh * 0.3f, 60f, 32)
            sr.end()
            game.batch.begin()
            font.color = Color(1f, 1f, 1f, 0.3f)
            font.draw(game.batch, "STEER", sw * 0.75f - 20f, sh * 0.3f + 8f)
            font.color = Color.WHITE
            game.batch.end()
        }
    }

    /**
     * Overlays for PARKED / CRASHED states.
     */
    private fun drawStateOverlay() {
        if (state == State.PLAYING) return

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val hudCam = OrthographicCamera(sw, sh)
        hudCam.position.set(sw / 2f, sh / 2f, 0f)
        hudCam.update()

        val sr = game.shapeRenderer
        sr.projectionMatrix = hudCam.combined

        // Dim overlay
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0f, 0f, 0f, 0.5f)
        sr.rect(0f, 0f, sw, sh)
        sr.end()

        game.batch.projectionMatrix = hudCam.combined
        game.batch.begin()

        when (state) {
            State.PARKED -> {
                font.color = Color.GREEN
                val msg = if (levelNumber < 5) "PARKED! Next level..." else "YOU WIN! 🎉"
                layout.setText(font, msg)
                font.draw(game.batch, msg, (sw - layout.width) / 2f, sh / 2f + 10f)
            }
            State.CRASHED -> {
                font.color = Color.RED
                val msg = "CRASH! Restarting..."
                layout.setText(font, msg)
                font.draw(game.batch, msg, (sw - layout.width) / 2f, sh / 2f + 10f)
            }
            else -> {}
        }
        font.color = Color.WHITE
        game.batch.end()
    }

    // -------------------------------------------------------------------------
    // Screen interface (unused but required)
    // -------------------------------------------------------------------------

    override fun hide() {}
    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        font.dispose()
    }
}
