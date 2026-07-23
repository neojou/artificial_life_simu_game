package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.SimConfig

/**
 * Headless simulation driver.
 *
 * Current scope: time + land + camp food stock. Agent AI and per-tick economy
 * wiring land in later milestones; callers may use [Economy] APIs directly.
 *
 * @property grid World map.
 * @property clock In-game calendar.
 * @property campFood Global food stored at the homestead (GDD §4.1).
 */
class SimulationEngine(
    val grid: Grid = Grid.createDefault(),
    val clock: Clock = Clock(),
    var campFood: Int = SimConfig.INITIAL_CAMP_FOOD,
) {
    init {
        require(campFood >= 0) { "campFood must be >= 0 (got $campFood)" }
    }
    /**
     * Advances one in-game hour.
     *
     * On day wrap (`hour` 5 → 0):
     * 1. Land ages and may transition (FARM↔EMPTY↔GRASS rules).
     * 2. Each remaining FARM tile gains daily yield.
     */
    fun stepHour() {
        val crossedIntoNewDay = clock.advanceHour()
        if (crossedIntoNewDay) {
            LandSystem.ageAndTransition(grid)
            LandSystem.applyDailyFarmYield(grid)
        }
    }

    /**
     * Advances [days] full days (`days * [SimConfig.HOURS_PER_DAY]` hours).
     */
    fun runDays(days: Int) {
        require(days >= 0) { "days must be >= 0 (got $days)" }
        repeat(days * SimConfig.HOURS_PER_DAY) {
            stepHour()
        }
    }
}
