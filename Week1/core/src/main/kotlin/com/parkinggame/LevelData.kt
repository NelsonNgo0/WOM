package com.parkinggame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle

/**
 * ParkingSpot.kt
 * Path: core/src/main/kotlin/com/parkinggame/ParkingSpot.kt
 *
 * Represents the target parking space the player must drift into.
 */
class ParkingSpot(x: Float, y: Float, width: Float = 50f, height: Float = 80f) {
    val bounds = Rectangle(x, y, width, height)
    var isOccupied = false    // True once the player successfully parks
}

/**
 * Obstacle.kt — inline in this file for brevity.
 * Represents a static collidable rectangle (wall, barrier, parked car, cone, etc.)
 */
data class Obstacle(
    val bounds: Rectangle,
    val color: Color = Color.DARK_GRAY
)

/**
 * LevelData.kt
 * Path: core/src/main/kotlin/com/parkinggame/LevelData.kt
 *
 * All 5 levels defined here. Each level has:
 *  - Car start position & angle
 *  - A list of obstacles
 *  - A parking spot (goal)
 *
 * World coordinates: 0,0 = bottom-left. The virtual viewport is 480 x 800.
 */
object LevelData {

    // Virtual screen dimensions (see GameScreen for camera setup)
    const val WORLD_WIDTH = 480f
    const val WORLD_HEIGHT = 800f

    data class Level(
        val carStartX: Float,
        val carStartY: Float,
        val carStartAngle: Float,       // Degrees. 90 = facing up
        val obstacles: List<Obstacle>,
        val parkingSpot: ParkingSpot,
        val roadColor: Color = Color(0.15f, 0.15f, 0.15f, 1f),
        val levelName: String = ""
    )

    fun get(levelNumber: Int): Level = when (levelNumber) {
        1 -> level1()
        2 -> level2()
        3 -> level3()
        4 -> level4()
        5 -> level5()
        else -> level1()
    }

    // -------------------------------------------------------------------------
    // LEVEL 1 — Straight road, simple park at top
    // -------------------------------------------------------------------------
    private fun level1() = Level(
        carStartX = WORLD_WIDTH / 2f,
        carStartY = 80f,
        carStartAngle = 90f,
        levelName = "Level 1: The Warm-Up",
        obstacles = listOf(
            // Left wall
            Obstacle(Rectangle(0f, 0f, 60f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            // Right wall
            Obstacle(Rectangle(WORLD_WIDTH - 60f, 0f, 60f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            // One barrier in the middle
            Obstacle(Rectangle(100f, 400f, 80f, 20f), Color(0.9f, 0.6f, 0.1f, 1f))
        ),
        parkingSpot = ParkingSpot(
            x = WORLD_WIDTH / 2f - 25f,
            y = WORLD_HEIGHT - 160f
        )
    )

    // -------------------------------------------------------------------------
    // LEVEL 2 — Slight zigzag, tighter spot
    // -------------------------------------------------------------------------
    private fun level2() = Level(
        carStartX = 120f,
        carStartY = 80f,
        carStartAngle = 90f,
        levelName = "Level 2: The Chicane",
        obstacles = listOf(
            Obstacle(Rectangle(0f, 0f, 60f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            Obstacle(Rectangle(WORLD_WIDTH - 60f, 0f, 60f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            // Chicane barriers — force the player to weave
            Obstacle(Rectangle(60f, 280f, 200f, 25f), Color(0.9f, 0.6f, 0.1f, 1f)),
            Obstacle(Rectangle(220f, 500f, 200f, 25f), Color(0.9f, 0.6f, 0.1f, 1f))
        ),
        parkingSpot = ParkingSpot(
            x = WORLD_WIDTH - 120f,
            y = WORLD_HEIGHT - 180f,
            width = 45f,
            height = 75f
        )
    )

    // -------------------------------------------------------------------------
    // LEVEL 3 — Parked cars on sides, narrow lane
    // -------------------------------------------------------------------------
    private fun level3() = Level(
        carStartX = WORLD_WIDTH / 2f,
        carStartY = 80f,
        carStartAngle = 90f,
        levelName = "Level 3: The Gauntlet",
        obstacles = listOf(
            Obstacle(Rectangle(0f, 0f, 60f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            Obstacle(Rectangle(WORLD_WIDTH - 60f, 0f, 60f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            // Parked cars on left side
            Obstacle(Rectangle(60f, 200f, 70f, 110f), Color(0.2f, 0.5f, 0.8f, 1f)),
            Obstacle(Rectangle(60f, 380f, 70f, 110f), Color(0.8f, 0.2f, 0.2f, 1f)),
            Obstacle(Rectangle(60f, 560f, 70f, 110f), Color(0.2f, 0.7f, 0.3f, 1f)),
            // Parked cars on right side
            Obstacle(Rectangle(WORLD_WIDTH - 130f, 300f, 70f, 110f), Color(0.7f, 0.7f, 0.2f, 1f)),
            Obstacle(Rectangle(WORLD_WIDTH - 130f, 480f, 70f, 110f), Color(0.6f, 0.2f, 0.7f, 1f))
        ),
        parkingSpot = ParkingSpot(
            x = WORLD_WIDTH / 2f - 22f,
            y = WORLD_HEIGHT - 200f,
            width = 44f,
            height = 72f
        )
    )

    // -------------------------------------------------------------------------
    // LEVEL 4 — Curved road feel (offset barriers)
    // -------------------------------------------------------------------------
    private fun level4() = Level(
        carStartX = 100f,
        carStartY = 80f,
        carStartAngle = 80f,
        levelName = "Level 4: The Curve",
        obstacles = listOf(
            Obstacle(Rectangle(0f, 0f, 55f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            Obstacle(Rectangle(WORLD_WIDTH - 55f, 0f, 55f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            // Inner curve obstacles
            Obstacle(Rectangle(200f, 150f, 180f, 25f), Color(0.9f, 0.6f, 0.1f, 1f)),
            Obstacle(Rectangle(60f, 350f, 160f, 25f), Color(0.9f, 0.6f, 0.1f, 1f)),
            Obstacle(Rectangle(240f, 550f, 140f, 25f), Color(0.9f, 0.6f, 0.1f, 1f)),
            // Traffic cone clusters
            Obstacle(Rectangle(130f, 620f, 25f, 25f), Color(1f, 0.4f, 0f, 1f)),
            Obstacle(Rectangle(160f, 640f, 25f, 25f), Color(1f, 0.4f, 0f, 1f))
        ),
        parkingSpot = ParkingSpot(
            x = 300f,
            y = WORLD_HEIGHT - 200f,
            width = 44f,
            height = 72f
        )
    )

    // -------------------------------------------------------------------------
    // LEVEL 5 — Tight multi-obstacle maze, small parking spot
    // -------------------------------------------------------------------------
    private fun level5() = Level(
        carStartX = WORLD_WIDTH / 2f,
        carStartY = 80f,
        carStartAngle = 90f,
        levelName = "Level 5: The Finale",
        obstacles = listOf(
            Obstacle(Rectangle(0f, 0f, 55f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            Obstacle(Rectangle(WORLD_WIDTH - 55f, 0f, 55f, WORLD_HEIGHT), Color(0.3f, 0.3f, 0.35f, 1f)),
            // Complex barrier maze
            Obstacle(Rectangle(55f, 200f, 130f, 22f), Color(0.9f, 0.6f, 0.1f, 1f)),
            Obstacle(Rectangle(295f, 280f, 130f, 22f), Color(0.9f, 0.6f, 0.1f, 1f)),
            Obstacle(Rectangle(55f, 400f, 160f, 22f), Color(0.9f, 0.6f, 0.1f, 1f)),
            Obstacle(Rectangle(265f, 480f, 160f, 22f), Color(0.9f, 0.6f, 0.1f, 1f)),
            Obstacle(Rectangle(55f, 580f, 120f, 22f), Color(0.9f, 0.6f, 0.1f, 1f)),
            // Parked cars
            Obstacle(Rectangle(310f, 560f, 65f, 100f), Color(0.2f, 0.5f, 0.8f, 1f)),
        ),
        parkingSpot = ParkingSpot(
            x = 160f,
            y = WORLD_HEIGHT - 180f,
            width = 40f,       // Extra tight!
            height = 68f
        )
    )
}
