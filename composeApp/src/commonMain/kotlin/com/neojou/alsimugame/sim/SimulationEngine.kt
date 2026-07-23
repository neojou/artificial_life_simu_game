package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.SimConfig

/**
 * Headless simulation driver (M1-T2 scope: time + land only).
 *
 * Agent AI, economy, and UI wiring are intentionally absent; later milestones
 * extend [stepHour] without changing the clock/land ordering defined here.
 *
 * @property grid World map.
 * @property clock In-game calendar.
 */
class SimulationEngine(
    val grid: Grid = Grid.createDefault(),
    val clock: Clock = Clock(),
) {
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
