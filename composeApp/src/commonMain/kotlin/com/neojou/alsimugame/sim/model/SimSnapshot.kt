package com.neojou.alsimugame.sim.model

import kotlinx.serialization.Serializable

/**
 * Immutable projection of one land cell for UI / determinism checks.
 */
@Serializable
data class TileSnapshot(
    val x: Int,
    val y: Int,
    val state: TileState,
    val ageDays: Int,
    val pendingHarvest: Int,
)

/**
 * Immutable projection of one agent.
 */
@Serializable
data class AgentSnapshot(
    val id: String,
    val gender: Gender,
    val x: Int,
    val y: Int,
    val stamina: Int,
    val carriedFood: Int,
    val ageDays: Int,
    val mode: AgentMode,
    val returnHome: Boolean,
)

/**
 * Immutable world view produced by [com.neojou.alsimugame.sim.SimulationEngine.snapshot].
 *
 * UI must read this type only — never mutate live engine objects.
 * Serializable so save/load and tests can round-trip a minimal encoding.
 */
@Serializable
data class SimSnapshot(
    val seed: Long,
    val day: Int,
    val hour: Int,
    val isDay: Boolean,
    val isNight: Boolean,
    val campFood: Int,
    val tiles: List<TileSnapshot>,
    val agents: List<AgentSnapshot>,
    val isGameOver: Boolean,
    /** Cumulative farm yield actually added to tiles (after pending cap). */
    val totalFoodProduced: Int = 0,
    /** Cumulative food harvested from tiles into agent carry. */
    val totalFoodHarvested: Int = 0,
    /** Cumulative food deposited into camp stock. */
    val totalFoodDeposited: Int = 0,
) {
    /** Count of peripheral tiles in each land state (camp omitted). */
    fun landStateCounts(): Map<TileState, Int> =
        tiles.groupingBy { it.state }.eachCount()

    /** Sum of pending harvest across all farm tiles. */
    fun totalPendingHarvest(): Int =
        tiles.sumOf { it.pendingHarvest }

    val livingAgentCount: Int
        get() = agents.count { it.mode != AgentMode.DEAD }

    val deadAgentCount: Int
        get() = agents.count { it.mode == AgentMode.DEAD }

    /** Average ageDays of living agents, or 0 if none. */
    fun averageLivingAgeDays(): Double {
        val living = agents.filter { it.mode != AgentMode.DEAD }
        if (living.isEmpty()) return 0.0
        return living.map { it.ageDays }.average()
    }
}
