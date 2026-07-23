package com.neojou.alsimugame.sim.ai

import com.neojou.alsimugame.sim.Clock
import com.neojou.alsimugame.sim.Economy
import com.neojou.alsimugame.sim.Grid
import com.neojou.alsimugame.sim.SimRng
import com.neojou.alsimugame.sim.model.Agent
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.TileState
import com.neojou.alsimugame.sim.path.Pathfinder

/**
 * Read-only view of the world used by [AgentBrain.decide].
 */
data class BrainWorld(
    val grid: Grid,
    val clock: Clock,
    val rng: SimRng,
)

/**
 * Priority-list villager AI (GDD §5, Architecture §6).
 *
 * Not a full behavior-tree framework — a single ordered decision per hour.
 */
object AgentBrain {

    /**
     * Stamina at or below this while away from camp triggers emergency return
     * (GDD §10 deadlock insurance).
     */
    const val LOW_STAMINA_THRESHOLD: Int = 1

    /**
     * Chooses the next [AgentAction] for [agent].
     * May update [Agent.mode] / [Agent.returnHome] as side effects of intent.
     */
    fun decide(agent: Agent, world: BrainWorld): AgentAction {
        if (!agent.isAlive) return AgentAction.None

        val night = world.clock.isNight
        val atCamp = agent.isAtCamp
        val size = world.grid.size

        // 1) Night: force return if not at camp
        if (night && !atCamp) {
            agent.mode = AgentMode.RETURNING
            agent.returnHome = true
            val step = Pathfinder.nextStepTowardCamp(agent.pos, size)
                ?: return AgentAction.None
            return AgentAction.Move(step, force = true)
        }

        // 2) Low stamina insurance while away
        if (agent.stamina <= LOW_STAMINA_THRESHOLD && !atCamp) {
            agent.mode = AgentMode.RETURNING
            agent.returnHome = true
            val step = Pathfinder.nextStepTowardCamp(agent.pos, size)
                ?: return AgentAction.None
            val force = agent.stamina < SimConfig.MOVE_STAMINA
            return AgentAction.Move(step, force = force)
        }

        // 3) Returning home
        if (agent.returnHome || agent.mode == AgentMode.RETURNING) {
            if (atCamp) {
                agent.returnHome = false
                agent.mode = AgentMode.SUPPLYING
                agent.path = emptyList()
                return AgentAction.SupplyAtCamp
            }
            agent.mode = AgentMode.RETURNING
            val step = Pathfinder.nextStepTowardCamp(agent.pos, size)
                ?: return AgentAction.None
            val force = night || agent.stamina < SimConfig.MOVE_STAMINA
            return AgentAction.Move(step, force = force)
        }

        // 4) At camp: rest at night; explore by day if any stamina
        if (atCamp) {
            if (night) {
                agent.mode = AgentMode.RESTING
                return AgentAction.Rest
            }
            if (agent.stamina > 0) {
                agent.mode = AgentMode.EXPLORING
                val exits = Pathfinder.neighbors(GridPos.CAMP, size)
                    .filter { !it.isCamp() }
                if (exits.isEmpty()) return AgentAction.Rest
                val dest = world.rng.nextFrom(exits)
                return AgentAction.Move(dest)
            }
            agent.mode = AgentMode.RESTING
            return AgentAction.Rest
        }

        // 5) On a land tile: work or keep exploring
        val tile = world.grid.tileAt(agent.pos)
        if (tile != null) {
            when {
                tile.state == TileState.GRASS &&
                    Economy.canAfford(agent, Economy.tillCost()) -> {
                    agent.mode = AgentMode.TILLING
                    return AgentAction.TillHere
                }
                tile.state == TileState.FARM && tile.pendingHarvest > 0 -> {
                    agent.mode = AgentMode.HARVESTING
                    return AgentAction.HarvestHere
                }
                else -> {
                    agent.mode = AgentMode.EXPLORING
                    return exploreStep(agent, world)
                }
            }
        }

        agent.mode = AgentMode.RESTING
        return AgentAction.Rest
    }

    /**
     * Continue exploration: pick an adjacent cell, preferring those farther
     * from camp so agents do not immediately bounce home empty-handed.
     */
    private fun exploreStep(agent: Agent, world: BrainWorld): AgentAction {
        val size = world.grid.size
        val candidates = Pathfinder.neighbors(agent.pos, size)
        if (candidates.isEmpty()) return AgentAction.None

        val peripheral = candidates.filter { !it.isCamp() }
        val pool = peripheral.ifEmpty { candidates }
        val maxDist = pool.maxOf { Pathfinder.chebyshev(it, GridPos.CAMP) }
        val farthest = pool.filter { Pathfinder.chebyshev(it, GridPos.CAMP) >= maxDist }
        val dest = world.rng.nextFrom(farthest)
        return AgentAction.Move(dest)
    }
}
