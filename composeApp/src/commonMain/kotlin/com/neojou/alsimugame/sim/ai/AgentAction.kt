package com.neojou.alsimugame.sim.ai

import com.neojou.alsimugame.sim.model.GridPos

/**
 * One-hour intent chosen by [AgentBrain] for a single agent.
 */
sealed class AgentAction {
    /** No world mutation this hour. */
    data object None : AgentAction()

    /**
     * Step to an adjacent [dest].
     * @property force When true, move even if stamina cannot cover the move cost
     * (night / emergency return — GDD §5.1).
     */
    data class Move(
        val dest: GridPos,
        val force: Boolean = false,
    ) : AgentAction()

    /** Till grassland under the agent (pays till stamina). */
    data object TillHere : AgentAction()

    /** Harvest farm tile under the agent (no stamina). */
    data object HarvestHere : AgentAction()

    /** Deposit carried food and refill stamina from camp stock. */
    data object SupplyAtCamp : AgentAction()

    /** Idle / sleep at camp. */
    data object Rest : AgentAction()
}
