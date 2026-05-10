package com.parkinggame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle

class ParkingSpot(x: Float, y: Float, width: Float = 60f, height: Float = 90f) {
    val bounds = Rectangle(x, y, width, height)
    var isOccupied = false
}

data class Obstacle(
    val bounds: Rectangle,
    val color: Color = Color(0.3f, 0.3f, 0.35f, 1f)
)

/**
 * LevelData.kt
 *
 * World is TALL — car starts at bottom, drives UP through a long road,
 * then hits a T-intersection at the top where the parking spot waits sideways.
 *
 * The camera scrolls with the car so the player never sees the spot until they arrive.
 *
 * World width: 480. Road runs up the centre (~160px wide between walls).
 * T-intersection at Y = ~1400. Total world height = ~1800.
 */
object LevelData {

    const val WORLD_WIDTH = 480f
    // Each level's world is tall enough to require scrolling
    const val WORLD_HEIGHT = 1800f
    const val ROAD_LEFT  = 100f     // Left kerb X
    const val ROAD_RIGHT = 380f     // Right kerb X
    const val ROAD_CENTRE = 240f

    data class Level(
        val carStartX: Float,
        val carStartY: Float,
        val carStartAngle: Float,
        val obstacles: List<Obstacle>,
        val parkingSpot: ParkingSpot,
        val worldHeight: Float,
        val levelName: String
    )

    fun get(n: Int): Level = when (n) {
        1 -> level1()
        2 -> level2()
        else -> level1()
    }

    // LEVEL 1 — Long straight road → T-intersection, park on the right
    private fun level1(): Level {
        val wh = 1800f
        val tY = 1650f          // Y of the T-intersection top wall

        val obstacles = mutableListOf<Obstacle>()
        val wallColor = Color(0.28f, 0.28f, 0.32f, 1f)
        val barrierColor = Color(0.9f, 0.6f, 0.1f, 1f)

        obstacles += Obstacle(Rectangle(0f, tY + 120f, WORLD_WIDTH, 40f), wallColor)


        // === Parking bay walls (horizontal slot to the RIGHT) ===
        val spotX = ROAD_CENTRE -30f
        val spotY = tY
        val spotW = 70f
        val spotH = 40f

        // Level 1 is obstacle-free — just learn to steer, brake, and drift!

        val spot = ParkingSpot(spotX, spotY, spotW, spotH)

        return Level(
            carStartX = ROAD_CENTRE,
            carStartY = 80f,
            carStartAngle = 90f,
            obstacles = obstacles,
            parkingSpot = spot,
            worldHeight = wh,
            levelName = "Level 1: The Drift-In"
        )
    }

    // -------------------------------------------------------------------------
    // LEVEL 2 — Chicane road → T-intersection, park on the LEFT
    // -------------------------------------------------------------------------
    private fun level2(): Level {
        val wh = 2000f
        val tY = 1600f
        val wallColor = Color(0.28f, 0.28f, 0.32f, 1f)
        val barrierColor = Color(0.9f, 0.6f, 0.1f, 1f)

        val obstacles = mutableListOf<Obstacle>()

        // Left wall
        obstacles += Obstacle(Rectangle(0f, 0f, ROAD_LEFT, tY + 40f), wallColor)
        // Right wall
        obstacles += Obstacle(Rectangle(ROAD_RIGHT, 0f, WORLD_WIDTH - ROAD_RIGHT, tY + 40f), wallColor)

        // Top walls — gap on LEFT this time
        obstacles += Obstacle(Rectangle(0f, tY + 40f, ROAD_LEFT - 30f, wh - tY), wallColor)
        obstacles += Obstacle(Rectangle(ROAD_RIGHT - 30f, tY + 40f, WORLD_WIDTH, wh - tY), wallColor)

        // Parking bay on the LEFT
        val spotX = 20f
        val spotY = tY - 50f
        val spotW = 130f
        val spotH = 75f

        obstacles += Obstacle(Rectangle(spotX, spotY + spotH, spotW, 20f), wallColor)
        obstacles += Obstacle(Rectangle(spotX, spotY - 20f, spotW, 20f), wallColor)
        obstacles += Obstacle(Rectangle(spotX - 20f, spotY - 20f, 20f, spotH + 40f), wallColor)

        // Chicane barriers
        obstacles += Obstacle(Rectangle(ROAD_LEFT + 10f, 350f, 100f, 22f), barrierColor)
        obstacles += Obstacle(Rectangle(ROAD_RIGHT - 110f, 650f, 100f, 22f), barrierColor)
        obstacles += Obstacle(Rectangle(ROAD_LEFT + 10f, 950f, 80f, 22f), barrierColor)
        obstacles += Obstacle(Rectangle(ROAD_RIGHT - 90f, 1250f, 80f, 22f), barrierColor)

        // Parked cars on sides
        obstacles += Obstacle(Rectangle(ROAD_LEFT + 5f, 500f, 60f, 95f), Color(0.2f, 0.5f, 0.8f, 1f))
        obstacles += Obstacle(Rectangle(ROAD_RIGHT - 65f, 800f, 60f, 95f), Color(0.8f, 0.3f, 0.2f, 1f))

        val spot = ParkingSpot(spotX + 5f, spotY, spotW - 15f, spotH)

        return Level(
            carStartX = ROAD_CENTRE,
            carStartY = 80f,
            carStartAngle = 90f,
            obstacles = obstacles,
            parkingSpot = spot,
            worldHeight = wh,
            levelName = "Level 2: The Chicane"
        )
    }
}