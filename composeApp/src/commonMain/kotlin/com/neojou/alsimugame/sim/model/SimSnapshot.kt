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
) {
    /** Count of peripheral tiles in each land state (camp omitted). */
    fun landStateCounts(): Map<TileState, Int> =
        tiles.groupingBy { it.state }.eachCount()

    /** Sum of pending harvest across all farm tiles. */
    fun totalPendingHarvest(): Int =
        tiles.sumOf { it.pendingHarvest }

    val livingAgentCount: Int
        get() = agents.count { it.mode != AgentMode.DEAD }
}
