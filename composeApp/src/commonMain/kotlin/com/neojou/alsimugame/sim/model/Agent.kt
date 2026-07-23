package com.neojou.alsimugame.sim.model

/**
 * An autonomous villager on the map.
 *
 * Mutable fields are updated by the simulation engine (later milestones).
 * Default values match GDD initial setup: full stamina, empty carry, resting at camp.
 *
 * @property id Stable identity for UI and tests.
 * @property gender Visual / identity distinction (v0.1: one male, one female).
 * @property pos Current cell.
 * @property stamina Remaining energy (0..[SimConfig.MAX_STAMINA]).
 * @property carriedFood Food held after harvest, deposited at camp.
 * @property ageDays Days lived (lifespan [SimConfig.LIFESPAN_DAYS]).
 * @property mode Current high-level behaviour mode.
 * @property returnHome When true, agent prioritizes returning to camp after work.
 * @property path Remaining path cells toward a goal (may be empty).
 */
data class Agent(
    val id: String,
    val gender: Gender,
    var pos: GridPos = GridPos.CAMP,
    var stamina: Int = SimConfig.MAX_STAMINA,
    var carriedFood: Int = 0,
    var ageDays: Int = 0,
    var mode: AgentMode = AgentMode.RESTING,
    var returnHome: Boolean = false,
    var path: List<GridPos> = emptyList(),
) {
    val isAlive: Boolean
        get() = mode != AgentMode.DEAD

    val isAtCamp: Boolean
        get() = pos.isCamp()
}
