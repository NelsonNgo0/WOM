package com.parkinggame

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

/**
 * Car.kt — Mario Kart style drift
 *
 * BEFORE BRAKE: car drives forward normally, builds speed
 * BRAKE TAP: locks into drift mode — rear slides out, car pivots
 * DURING DRIFT: steering rotates the car fast but velocity bleeds sideways
 * BRAKE RELEASED: drift ends, car coasts to stop (no more acceleration ever)
 * ONE BRAKE PER LEVEL — use it wisely
 */
class Car(startX: Float, startY: Float, startAngle: Float = 90f) {

    val position = Vector2(startX, startY)
    var angle = startAngle

    val width = 28f
    val height = 52f

    var speed = STARTING_SPEED
    var isBraking = false
    var isDrifting = false
    var isParked = false
    var brakeUsed = false           // True once brake has ever been touched

    // Actual movement vector — separates from facing when drifting
    val velocity = Vector2(
        MathUtils.cosDeg(startAngle) * STARTING_SPEED,
        MathUtils.sinDeg(startAngle) * STARTING_SPEED
    )

    var steerInput = 0f

    val bounds: Rectangle
        get() = Rectangle(position.x - width / 2f, position.y - height / 2f, width, height)

    companion object {
        const val STARTING_SPEED = 220f     // Car always starts moving
        const val MAX_SPEED = 400f
        const val ACCEL = 55f               // Speed buildup before brake
        const val BRAKE_DECEL = 450f        // How hard braking slows you
        const val COAST_DECEL = 80f         // Slow bleed after brake released

        const val NORMAL_STEER = 85f        // Degrees/sec normal steering
        const val DRIFT_STEER = 90f        // steer sens for drifting

        // Grip: 1.0 = velocity instantly matches facing, 0.0 = pure ice
        const val NORMAL_GRIP = 0.88f
        const val DRIFT_GRIP = 0.30f        // Very low — rear really slides out

        const val MIN_DRIFT_SPEED = 180f    // Must be going this fast to drift
    }

    fun update(delta: Float, braking: Boolean, steer: Float) {
        steerInput = steer

        // ── 1. One-shot brake logic ──────────────────────────────────────────
        if (braking && !brakeUsed) {
            // First ever brake press this level
            brakeUsed = true
        }

        val activeBrake = brakeUsed && braking   // True only while finger is held

        isBraking = activeBrake

        // ── 2. Drift state ───────────────────────────────────────────────────
        // Lock into drift when: brake held + fast enough
        // Drift stays locked until brake is released, regardless of speed drop
        isDrifting = activeBrake

        // ── 3. Speed ─────────────────────────────────────────────────────────
        speed = when {
            isDrifting ->
                // Brake is slowing us during drift
                (speed - BRAKE_DECEL * delta).coerceAtLeast(0f)
            !brakeUsed ->
                // Haven't braked yet — accelerate normally
                (speed + ACCEL * delta).coerceAtMost(MAX_SPEED)
            else ->
                // Brake was used and released — coast to stop, no more power
                (speed - COAST_DECEL * delta).coerceAtLeast(0f)
        }

        // ── 4. Steering ──────────────────────────────────────────────────────
        // During drift: much faster rotation (the car pivots dramatically)
        // Outside drift: normal steering
        val steerRate = if (isDrifting) DRIFT_STEER else NORMAL_STEER
        val speedFactor = if (isDrifting) 0.8f else (speed / MAX_SPEED).coerceIn(0.2f, 1f)
        angle -= steer * steerRate * speedFactor * delta
        angle = ((angle % 360f) + 360f) % 360f

        // ── 5. Velocity direction (the drift effect) ─────────────────────────
        // Desired = car is pointing this way at this speed
        val rad = angle * MathUtils.degreesToRadians
        val desiredVel = Vector2(
            MathUtils.cos(rad) * speed,
            MathUtils.sin(rad) * speed
        )

        // Actual velocity lerps toward desired
        // Low grip during drift = velocity LAGS behind facing = sideways slide
        val grip = if (isDrifting) DRIFT_GRIP else NORMAL_GRIP
        velocity.lerp(desiredVel, 1f - grip)

        // ── 6. Move ──────────────────────────────────────────────────────────
        position.x += velocity.x * delta
        position.y += velocity.y * delta
    }

    fun collidesWith(obstacle: Rectangle): Boolean = bounds.overlaps(obstacle)

    fun isInsideSpot(spot: ParkingSpot): Boolean {
        if (speed > 130f) return false
        val cb = bounds
        val sb = spot.bounds
        val ox = minOf(cb.x + cb.width, sb.x + sb.width) - maxOf(cb.x, sb.x)
        val oy = minOf(cb.y + cb.height, sb.y + sb.height) - maxOf(cb.y, sb.y)
        if (ox <= 0 || oy <= 0) return false
        val overlap = ox * oy
        val carArea = cb.width * cb.height
        return (overlap / carArea) >= 0.65f
    }
}