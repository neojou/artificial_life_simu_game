package com.neojou.alsimugame.sim.model

/**
 * High-level behaviour mode for a villager agent (GDD §5).
 *
 * Decision logic will live in the AI layer (AgentBrain); this enum is the
 * shared vocabulary for simulation and UI.
 */
enum class AgentMode {
    /** At camp, idle or sleeping. */
    RESTING,

    /** Depositing food and refilling stamina at the start of a day. */
    SUPPLYING,

    /** Daytime exploration on peripheral tiles. */
    EXPLORING,

    /** Converting grassland into farmland. */
    TILLING,

    /** Collecting pending harvest from farmland. */
    HARVESTING,

    /** Pathing back to camp. */
    RETURNING,

    /** No longer active in the simulation. */
    DEAD,
}
