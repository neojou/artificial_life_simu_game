package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.Agent
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.Tile
import com.neojou.alsimugame.sim.model.TileState
import kotlin.math.min

/**
 * Stamina and hour cost of a discrete simulation action.
 *
 * Hour costs are informational for the engine tick model (one action per hour tick);
 * [Economy] applies stamina immediately when callers use [spendStamina] / action helpers.
 *
 * @property stamina Stamina deducted from the agent (0 for harvest).
 * @property hours In-game hours the action consumes.
 */
data class ActionCost(
    val stamina: Int,
    val hours: Int,
) {
    init {
        require(stamina >= 0) { "stamina cost must be >= 0 (got $stamina)" }
        require(hours >= 0) { "hours cost must be >= 0 (got $hours)" }
    }
}

/**
 * Camp food stock, stamina supply, action costs, deposit, and harvest transfer.
 *
 * All numeric rules read from [SimConfig] (GDD §4 / §11). Pure functions are
 * side-effect free except where they intentionally mutate [Agent] / [Tile].
 */
object Economy {

    /** Cost to move one cell (including diagonal). */
    fun moveCost(): ActionCost =
        ActionCost(
            stamina = SimConfig.MOVE_STAMINA,
            hours = SimConfig.MOVE_HOURS,
        )

    /**
     * Extra cost to till grassland into farmland (after arriving on the tile).
     * Does not include the move that reached the tile.
     */
    fun tillCost(): ActionCost =
        ActionCost(
            stamina = SimConfig.TILL_EXTRA_STAMINA,
            hours = SimConfig.TILL_HOURS,
        )

    /** Cost to harvest a farm tile (time only; no stamina). */
    fun harvestCost(): ActionCost =
        ActionCost(
            stamina = 0,
            hours = SimConfig.HARVEST_HOURS,
        )

    /** Whether [agent] has enough stamina to pay [cost]. */
    fun canAfford(agent: Agent, cost: ActionCost): Boolean =
        agent.isAlive && agent.stamina >= cost.stamina

    /**
     * Deducts [cost].stamina from [agent] if affordable.
     *
     * @return `true` if stamina was spent; `false` if insufficient (agent unchanged).
     */
    fun spendStamina(agent: Agent, cost: ActionCost): Boolean {
        if (!canAfford(agent, cost)) return false
        agent.stamina -= cost.stamina
        return true
    }

    /**
     * Deposits all [Agent.carriedFood] into the camp stock.
     *
     * @param campFood Current global camp food.
     * @return Updated camp food after deposit; agent carry becomes 0.
     */
    fun depositCarriedFood(agent: Agent, campFood: Int): Int {
        require(campFood >= 0) { "campFood must be >= 0 (got $campFood)" }
        val updated = campFood + agent.carriedFood
        agent.carriedFood = 0
        return updated
    }

    /**
     * Spends camp food to restore [agent] stamina toward [SimConfig.MAX_STAMINA].
     *
     * Rule: each unit of food restores [SimConfig.FOOD_TO_STAMINA] points, never above max.
     * Consumes food one unit at a time until the agent is full or stock is empty
     * (partial supply when food is scarce).
     *
     * @param campFood Current global camp food.
     * @return Remaining camp food after supply.
     */
    fun supplyStaminaFromCamp(agent: Agent, campFood: Int): Int {
        require(campFood >= 0) { "campFood must be >= 0 (got $campFood)" }
        if (!agent.isAlive) return campFood

        var food = campFood
        while (agent.stamina < SimConfig.MAX_STAMINA && food > 0) {
            food -= 1
            agent.stamina = min(
                SimConfig.MAX_STAMINA,
                agent.stamina + SimConfig.FOOD_TO_STAMINA,
            )
        }
        return food
    }

    /**
     * How many food units would be consumed to fully top up [agent] (ignoring stock).
     * Useful for tests and UI hints.
     */
    fun foodUnitsNeededToFill(agent: Agent): Int {
        val need = SimConfig.MAX_STAMINA - agent.stamina
        if (need <= 0) return 0
        return (need + SimConfig.FOOD_TO_STAMINA - 1) / SimConfig.FOOD_TO_STAMINA
    }

    /**
     * Passive night rest at camp: +[SimConfig.NIGHT_REST_STAMINA], capped at max.
     * No-op if agent is dead or not at camp.
     */
    fun applyNightRest(agent: Agent) {
        if (!agent.isAlive || !agent.isAtCamp) return
        agent.stamina = min(
            SimConfig.MAX_STAMINA,
            agent.stamina + SimConfig.NIGHT_REST_STAMINA,
        )
    }

    /**
     * Transfers all [Tile.pendingHarvest] into [Agent.carriedFood] and clears pending.
     *
     * Does not spend stamina (see [harvestCost]); caller is responsible for the hour tick.
     *
     * @return Amount harvested (0 if none).
     * @throws IllegalArgumentException if tile is not [TileState.FARM].
     */
    fun harvest(agent: Agent, tile: Tile): Int {
        require(tile.state == TileState.FARM) {
            "Only FARM tiles can be harvested (was ${tile.state})"
        }
        val amount = tile.pendingHarvest
        agent.carriedFood += amount
        tile.pendingHarvest = 0
        return amount
    }

    /**
     * Applies [moveCost] stamina if affordable.
     * Does not change position (pathfinding is M2).
     */
    fun tryPayMove(agent: Agent): Boolean = spendStamina(agent, moveCost())

    /**
     * Applies [tillCost] stamina if affordable, then converts [tile] via [LandSystem.till].
     *
     * @return `true` if till succeeded; `false` if stamina insufficient (tile unchanged).
     */
    fun tryPayAndTill(agent: Agent, tile: Tile): Boolean {
        if (tile.state != TileState.GRASS) return false
        if (!spendStamina(agent, tillCost())) return false
        LandSystem.till(tile)
        return true
    }

    /**
     * Harvests [tile] into [agent] carry. Always succeeds if tile is FARM
     * (harvest has zero stamina cost).
     *
     * @return Amount harvested, or `null` if tile is not farmable.
     */
    fun tryHarvest(agent: Agent, tile: Tile): Int? {
        if (tile.state != TileState.FARM) return null
        return harvest(agent, tile)
    }
}
