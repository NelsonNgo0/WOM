package com.parkinggame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

/**
 * TouchInputHandler.kt
 * Path: core/src/main/kotlin/com/parkinggame/TouchInputHandler.kt
 *
 * Handles two-zone touch input:
 *
 *  LEFT HALF OF SCREEN  → Brake zone
 *                          Any finger touching the left half = braking
 *
 *  RIGHT HALF OF SCREEN → Virtual joystick
 *                          Touch down anchors the joystick center.
 *                          Drag left → car turns RIGHT (inverted as specified)
 *                          Drag right → car turns LEFT
 *
 * LibGDX screen coords: (0,0) = top-left, Y increases downward.
 * We convert to "game coords" for display but the raw logic is the same.
 */
class TouchInputHandler : InputAdapter() {

    // --- Brake state ---
    var isBraking = false
        private set

    // --- Joystick state ---
    // steerValue: -1.0 = full left turn, +1.0 = full right turn, 0 = straight
    var steerValue = 0f
        private set

    // Joystick touch tracking
    private var steerPointer = -1          // Which finger is steering (-1 = none)
    private var brakePointer = -1          // Which finger is braking (-1 = none)
    private val joystickAnchor = Vector2() // Where the joystick was first touched
    private val joystickCurrent = Vector2()// Where the joystick finger is now

    // Max drag distance (pixels) for full steer — adjust for feel
    private val maxJoystickRadius = 130f

    // Screen half-width used to separate brake/steer zones
    private val halfScreenWidth get() = Gdx.graphics.width / 2f

    // -------------------------------------------------------------------------
    // InputAdapter overrides
    // -------------------------------------------------------------------------

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val x = screenX.toFloat()

        if (x < halfScreenWidth) {
            // LEFT side → brake
            if (brakePointer == -1) {
                brakePointer = pointer
                isBraking = true
            }
        } else {
            // RIGHT side → joystick
            if (steerPointer == -1) {
                steerPointer = pointer
                joystickAnchor.set(x, screenY.toFloat())
                joystickCurrent.set(x, screenY.toFloat())
                steerValue = 0f
            }
        }
        return true   // Consume the event
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer == steerPointer) {
            joystickCurrent.set(screenX.toFloat(), screenY.toFloat())
            updateSteer()
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == brakePointer) {
            brakePointer = -1
            isBraking = false
        }
        if (pointer == steerPointer) {
            steerPointer = -1
            steerValue = 0f   // Auto-center when finger lifts
        }
        return true
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Computes steerValue from joystick drag delta.
     * Horizontal drag:
     *   Drag LEFT (negative dx) → steerValue positive (car turns RIGHT) — per spec
     *   Drag RIGHT (positive dx) → steerValue negative (car turns LEFT)
     */
    private fun updateSteer() {
        val dx = joystickCurrent.x - joystickAnchor.x
        // Clamp and normalise to -1..+1
        // Invert dx so dragging left = turning right
        val normalised = MathUtils.clamp(-dx / maxJoystickRadius, -1f, 1f)
        steerValue = normalised
    }

    // -------------------------------------------------------------------------
    // Rendering helpers (called from GameScreen to draw the UI overlays)
    // -------------------------------------------------------------------------

    /** Returns the joystick anchor position in SCREEN pixels (for drawing the UI ring). */
    fun getJoystickAnchor(): Vector2? =
        if (steerPointer != -1) Vector2(joystickAnchor) else null

    /** Returns the joystick knob position in SCREEN pixels. */
    fun getJoystickKnob(): Vector2? =
        if (steerPointer != -1) Vector2(joystickCurrent) else null

    /** True if any brake touch is active. */
    fun isBrakeTouched() = brakePointer != -1
}
