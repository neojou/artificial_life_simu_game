package com.neojou.alsimugame.sim.model

/**
 * Global simulation parameters for Tiny Camp v0.1.
 *
 * Single source of truth aligned with [game_design.md](GDD) §11.
 * Changing a value here requires updating the GDD and related tests.
 */
object SimConfig {
    /** Maximum agent stamina. */
    const val MAX_STAMINA: Int = 10

    /** Stamina restored per 1 unit of camp food consumed. */
    const val FOOD_TO_STAMINA: Int = 3

    /** Food produced by each FARM tile at the start of a new day. */
    const val FARM_YIELD_PER_DAY: Int = 1

    /** Stamina cost to move one cell (8-direction). */
    const val MOVE_STAMINA: Int = 1

    /** Hours consumed by one move step. */
    const val MOVE_HOURS: Int = 1

    /** Extra stamina cost when tilling grassland into farmland. */
    const val TILL_EXTRA_STAMINA: Int = 1

    /** Hours consumed by a till action. */
    const val TILL_HOURS: Int = 1

    /** Hours consumed by a harvest action (no stamina cost). */
    const val HARVEST_HOURS: Int = 1

    /** Days a land tile stays in FARM or EMPTY before transitioning. */
    const val LAND_STATE_DAYS: Int = 12

    /** Starting global food stock at the camp. */
    const val INITIAL_CAMP_FOOD: Int = 10

    /** Agent lifespan in days (fixed for v0.1). */
    const val LIFESPAN_DAYS: Int = 12

    /**
     * Hours per in-game day.
     * Hours `0..2` are day; `3..5` are night.
     */
    const val HOURS_PER_DAY: Int = 6

    /** Side length of the square map (3 → 3×3). */
    const val GRID_SIZE: Int = 3

    /** Camp (home base) cell coordinates. */
    const val CAMP_X: Int = 1
    const val CAMP_Y: Int = 1

    /** Passive stamina restored when resting at camp overnight (GDD §4.2). */
    const val NIGHT_REST_STAMINA: Int = 2
}
