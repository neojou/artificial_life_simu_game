package com.neojou.alsimugame.sim.model

/**
 * A tillable land cell on the peripheral map (not the camp).
 *
 * The camp cell is not represented as a [Tile].
 *
 * @property state Current land lifecycle state.
 * @property ageDays Days spent in the current [state] (incremented on day boundaries).
 * @property pendingHarvest Uncollected yield on [TileState.FARM] tiles;
 *   capped at [SimConfig.MAX_PENDING_HARVEST] when produced daily.
 */
data class Tile(
    var state: TileState = TileState.GRASS,
    var ageDays: Int = 0,
    var pendingHarvest: Int = 0,
) {
    /** Whether this tile currently holds harvestable food. */
    val hasHarvest: Boolean
        get() = state == TileState.FARM && pendingHarvest > 0
}
