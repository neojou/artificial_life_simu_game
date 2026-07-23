package com.neojou.alsimugame.sim.model

import kotlinx.serialization.Serializable

/**
 * Lifecycle state of a peripheral land tile.
 *
 * Transitions (see GDD land rules):
 * - [GRASS] → till → [FARM]
 * - [FARM] after [SimConfig.LAND_STATE_DAYS] → [EMPTY]
 * - [EMPTY] after [SimConfig.LAND_STATE_DAYS] → [GRASS]
 */
@Serializable
enum class TileState {
    /** Wild grassland; can be tilled into farmland. */
    GRASS,

    /** Productive farmland; accumulates [Tile.pendingHarvest]. */
    FARM,

    /** Fallow / exhausted land; recovers into grassland over time. */
    EMPTY,
}
