package com.parkinggame

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

/**
 * Car.kt
 * Path: core/src/main/kotlin/com/parkinggame/Car.kt
 *
 * Represents the player's car with arcade physics.
 * - Always moves forward (speed never goes below MIN_SPEED)
 * - Steering rotates the heading
 * - Braking reduces speed temporarily
 * - Drifting: steering at high speed causes the car to slide sideways
 */
class Car(startX: Float, startY: Float, startAngle: Float = 90f) {

    // --- Position & Orientation ---
    val position = Vector2(startX, startY)   // Center of the car (world units)
    var angle = startAngle                    // Degrees. 0=right, 90=up, 180=left, 270=down

    // --- Size (world units, think of 1 unit = 1 pixel at base resolution) ---
    val width = 30f
    val height = 55f

    // --- Speed ---
    var speed = MIN_SPEED                     // Current forward speed (units/second)
    private var isBraking = false

    // --- Drift state ---
    // The car's actual movement direction (may differ from facing when drifting)
    private val velocity = Vector2(0f, MIN_SPEED)  // Actual movement vector

    // --- Steering ---
    var steerInput = 0f   // -1.0 = full left, +1.0 = full right, 0 = straight

    // --- Collision bounds ---
    // A simple rectangle around the car (not rotated — "AABB" collision)
    val bounds: Rectangle get() = Rectangle(
        position.x - width / 2f,
        position.y - height / 2f,
        width,
        height
    )

    // --- Level completion flag ---
    var isParked = false

    companion object {
        const val MIN_SPEED = 80f          // Always rolling forward (units/sec)
        const val MAX_SPEED = 300f
        const val BRAKE_DECEL = 200f       // How fast braking slows you down
        const val NATURAL_ACCEL = 50f      // Gentle speed increase when not braking
        const val STEER_RATE = 120f        // Degrees per second of rotation
        const val DRIFT_FACTOR = 0.85f     // How much velocity "bleeds" sideways (0=no drift, 1=full slide)
        const val DRIFT_THRESHOLD = 200f   // Speed above which drifting kicks in
    }

    /**
     * Call every frame with delta time (seconds since last frame).
     * @param delta  Time step in seconds (LibGDX gives you this in render())
     * @param braking  True when the player is holding the brake
     * @param steer    Steering input: -1 (left) to +1 (right), 0 = straight
     */
    fun update(delta: Float, braking: Boolean, steer: Float) {
        isBraking = braking
        steerInput = steer

        // --- 1. Update speed ---
        if (braking) {
            speed -= BRAKE_DECEL * delta
        } else {
            speed += NATURAL_ACCEL * delta
        }
        speed = MathUtils.clamp(speed, MIN_SPEED, MAX_SPEED)

        // --- 2. Rotate the car based on steering ---
        // More steering effect at higher speeds (feels more responsive)
        val speedFactor = speed / MAX_SPEED
        angle -= steer * STEER_RATE * speedFactor * delta   // Negative = turns clockwise on screen

        // Keep angle in 0..360 range
        angle = ((angle % 360f) + 360f) % 360f

        // --- 3. Compute desired velocity (pointing in the car's facing direction) ---
        val facingRad = angle * MathUtils.degreesToRadians
        val desiredVelocity = Vector2(
            MathUtils.cos(facingRad) * speed,
            MathUtils.sin(facingRad) * speed
        )

        // --- 4. Apply drift (lerp actual velocity toward desired velocity) ---
        // At low speed: snappy (no drift). At high speed: more slide.
        val driftLerp = if (speed > DRIFT_THRESHOLD) DRIFT_FACTOR else 0.95f
        velocity.lerp(desiredVelocity, 1f - driftLerp)

        // --- 5. Move the car ---
        position.x += velocity.x * delta
        position.y += velocity.y * delta
    }

    /**
     * Simple AABB collision check against a rectangle obstacle.
     * Returns true if the car overlaps the obstacle.
     */
    fun collidesWith(obstacle: Rectangle): Boolean {
        return bounds.overlaps(obstacle)
    }

    /**
     * Check if the car is sufficiently inside the parking spot to count as parked.
     * The car must be mostly inside (80% overlap) and moving slowly.
     */
    fun isInsideSpot(spot: ParkingSpot): Boolean {
        if (speed > 120f) return false   // Too fast to park

        val carBounds = bounds
        val spotBounds = spot.bounds

        // Calculate overlap area
        val overlapX = minOf(carBounds.x + carBounds.width, spotBounds.x + spotBounds.width) -
                       maxOf(carBounds.x, spotBounds.x)
        val overlapY = minOf(carBounds.y + carBounds.height, spotBounds.y + spotBounds.height) -
                       maxOf(carBounds.y, spotBounds.y)

        if (overlapX <= 0 || overlapY <= 0) return false

        val overlapArea = overlapX * overlapY
        val carArea = carBounds.width * carBounds.height

        return (overlapArea / carArea) >= 0.7f   // 70% of car must be inside
    }
}
