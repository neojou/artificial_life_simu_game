package com.neojou.alsimugame.sim

/**
 * Minimal lifetime counters for the stats panel (GDD §6.3).
 *
 * Does not store history (replay is M6); only cumulative totals.
 */
class StatsRecorder {
    /** Food actually added to farm pending by daily yield (after cap). */
    var totalFoodProduced: Int = 0
        private set

    /** Food taken from tiles into agent carry via harvest. */
    var totalFoodHarvested: Int = 0
        private set

    /** Food deposited from agents into camp stock. */
    var totalFoodDeposited: Int = 0
        private set

    fun recordYield(amount: Int) {
        if (amount > 0) totalFoodProduced += amount
    }

    fun recordHarvest(amount: Int) {
        if (amount > 0) totalFoodHarvested += amount
    }

    fun recordDeposit(amount: Int) {
        if (amount > 0) totalFoodDeposited += amount
    }

    fun reset() {
        totalFoodProduced = 0
        totalFoodHarvested = 0
        totalFoodDeposited = 0
    }
}
