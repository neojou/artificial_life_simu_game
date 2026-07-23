package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.Tile
import com.neojou.alsimugame.sim.model.TileState
import kotlin.math.min

/**
 * Pure land rules: aging, state transitions, tilling, and daily farm yield.
 *
 * Camp cells are never touched (they have no [Tile] on [Grid]).
 *
 * Day-boundary order (see Architecture §4–5):
 * 1. [ageAndTransition] — every peripheral tile ages; FARM/EMPTY may change state.
 * 2. [applyDailyFarmYield] — remaining FARM tiles gain [SimConfig.FARM_YIELD_PER_DAY].
 */
object LandSystem {

    /**
     * Increments [Tile.ageDays] on every peripheral tile, then applies GDD transitions:
     * - FARM after [SimConfig.LAND_STATE_DAYS] → EMPTY (pending cleared)
     * - EMPTY after [SimConfig.LAND_STATE_DAYS] → GRASS
     * - GRASS does not auto-transition (only via [till])
     */
    fun ageAndTransition(grid: Grid) {
        grid.forEachPeripheral { _, tile ->
            tile.ageDays += 1
            when (tile.state) {
                TileState.GRASS -> {
                    // Age is tracked for consistency; no automatic transition.
                }
                TileState.FARM -> {
                    if (tile.ageDays >= SimConfig.LAND_STATE_DAYS) {
                        tile.state = TileState.EMPTY
                        tile.ageDays = 0
                        tile.pendingHarvest = 0
                    }
                }
                TileState.EMPTY -> {
                    if (tile.ageDays >= SimConfig.LAND_STATE_DAYS) {
                        tile.state = TileState.GRASS
                        tile.ageDays = 0
                    }
                }
            }
        }
    }

    /**
     * At the start of a new day, each FARM tile accumulates yield into [Tile.pendingHarvest],
     * capped at [SimConfig.MAX_PENDING_HARVEST].
     */
    fun applyDailyFarmYield(grid: Grid) {
        grid.forEachPeripheral { _, tile ->
            if (tile.state == TileState.FARM) {
                tile.pendingHarvest = min(
                    tile.pendingHarvest + SimConfig.FARM_YIELD_PER_DAY,
                    SimConfig.MAX_PENDING_HARVEST,
                )
            }
        }
    }

    /**
     * Immediately converts grassland into farmland (agent till action).
     *
     * Resets age to 0; does not grant same-tick yield (yield applies on day boundaries only).
     *
     * @throws IllegalArgumentException if [tile] is not [TileState.GRASS].
     */
    fun till(tile: Tile) {
        require(tile.state == TileState.GRASS) {
            "Only GRASS can be tilled (was ${tile.state})"
        }
        tile.state = TileState.FARM
        tile.ageDays = 0
    }
}
