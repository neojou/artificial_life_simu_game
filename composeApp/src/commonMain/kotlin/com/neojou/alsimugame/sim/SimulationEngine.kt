package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.ai.AgentAction
import com.neojou.alsimugame.sim.ai.AgentBrain
import com.neojou.alsimugame.sim.ai.BrainWorld
import com.neojou.alsimugame.sim.model.Agent
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.sim.model.TileSnapshot
import com.neojou.alsimugame.sim.path.Pathfinder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Headless simulation driver for Tiny Camp.
 *
 * Responsibilities:
 * - Seeded [SimRng] and default world (grass ring, camp food, two villagers)
 * - Time / land progression
 * - Agent movement ([tryMove]) and priority AI ([AgentBrain])
 * - Lifespan / stranded-at-night death and [isGameOver]
 * - Immutable [snapshot] for UI and determinism checks
 */
class SimulationEngine(
    val seed: Long = 0L,
    val grid: Grid = Grid.createDefault(),
    val clock: Clock = Clock(),
    var campFood: Int = SimConfig.INITIAL_CAMP_FOOD,
    agents: List<Agent> = defaultAgents(),
) {
    val rng: SimRng = SimRng(seed)

    val agents: MutableList<Agent> = agents.toMutableList()

    /**
     * When false, [stepHour] only advances time/land (and lifespan aging),
     * without [AgentBrain] actions. Useful for pure land unit tests.
     */
    var aiEnabled: Boolean = true

    init {
        require(campFood >= 0) { "campFood must be >= 0 (got $campFood)" }
    }

    val isGameOver: Boolean
        get() = agents.none { it.isAlive }

    /**
     * Attempts a single adjacent step for [agent] to [dest].
     *
     * Fails when: dead, out of bounds, not adjacent, or (when [force] is false)
     * insufficient stamina. When [force] is true, stamina is spent if possible
     * but the move still proceeds (night emergency return).
     *
     * @return `true` if position changed.
     */
    fun tryMove(agent: Agent, dest: GridPos, force: Boolean = false): Boolean {
        if (!agent.isAlive) return false
        if (!grid.isInBounds(dest)) return false
        if (!Pathfinder.areAdjacent(agent.pos, dest)) return false

        if (force) {
            Economy.tryPayMove(agent)
            agent.pos = dest
            return true
        }
        if (!Economy.tryPayMove(agent)) return false
        agent.pos = dest
        return true
    }

    /**
     * Advances one in-game hour.
     *
     * 1. Advance clock; on day wrap: land aging/yield, agent age, lifespan death,
     *    morning deposit+supply for agents at camp.
     * 2. Each living agent: [AgentBrain.decide] + execute (one action / hour).
     * 3. Night rest at camp when hour enters night; stranded-at-night death check.
     */
    fun stepHour() {
        val crossedIntoNewDay = clock.advanceHour()
        if (crossedIntoNewDay) {
            onNewDay()
        }

        if (!aiEnabled || isGameOver) return

        // Entering night: passive rest for agents already at camp (once per night).
        if (clock.hour == Clock.DAY_HOUR_COUNT) {
            for (agent in agents) {
                if (agent.isAlive && agent.isAtCamp) {
                    Economy.applyNightRest(agent)
                }
            }
        }

        val world = BrainWorld(grid = grid, clock = clock, rng = rng)
        for (agent in agents) {
            if (!agent.isAlive) continue
            val action = AgentBrain.decide(agent, world)
            execute(agent, action)
            applyStrandedNightDeath(agent)
        }
    }

    private fun onNewDay() {
        LandSystem.ageAndTransition(grid)
        LandSystem.applyDailyFarmYield(grid)

        for (agent in agents) {
            if (!agent.isAlive) continue
            agent.ageDays += 1
            if (agent.ageDays >= SimConfig.LIFESPAN_DAYS) {
                kill(agent, reason = "lifespan")
            }
        }

        // Morning supply for living agents at camp (GDD §5.1 daily supply).
        for (agent in agents) {
            if (!agent.isAlive || !agent.isAtCamp) continue
            campFood = Economy.depositCarriedFood(agent, campFood)
            campFood = Economy.supplyStaminaFromCamp(agent, campFood)
            agent.mode = AgentMode.RESTING
        }
    }

    private fun execute(agent: Agent, action: AgentAction) {
        when (action) {
            AgentAction.None -> Unit
            is AgentAction.Move -> {
                val moved = tryMove(agent, action.dest, force = action.force)
                if (moved && agent.mode != AgentMode.RETURNING && agent.mode != AgentMode.TILLING) {
                    // Keep EXPLORING after a successful scout step.
                    if (!agent.returnHome) {
                        agent.mode = AgentMode.EXPLORING
                    }
                }
            }
            AgentAction.TillHere -> {
                val tile = grid.tileAt(agent.pos) ?: return
                if (Economy.tryPayAndTill(agent, tile)) {
                    agent.returnHome = true
                    agent.mode = AgentMode.RETURNING
                }
            }
            AgentAction.HarvestHere -> {
                val tile = grid.tileAt(agent.pos) ?: return
                if (Economy.tryHarvest(agent, tile) != null) {
                    agent.returnHome = true
                    agent.mode = AgentMode.RETURNING
                }
            }
            AgentAction.SupplyAtCamp -> {
                if (!agent.isAtCamp) return
                campFood = Economy.depositCarriedFood(agent, campFood)
                campFood = Economy.supplyStaminaFromCamp(agent, campFood)
                agent.mode = AgentMode.RESTING
                agent.returnHome = false
            }
            AgentAction.Rest -> {
                agent.mode = AgentMode.RESTING
            }
        }
    }

    /**
     * Stranded-at-night rule (GDD §4.2, adapted for multi-step maps):
     * if it is night, stamina is 0, the agent is not at camp, **and** they are not
     * actively force-returning, they die.
     *
     * Agents in [AgentMode.RETURNING] / [Agent.returnHome] may need several hours
     * to walk home on a 5×5 map; they are not killed mid-path.
     */
    private fun applyStrandedNightDeath(agent: Agent) {
        if (!agent.isAlive) return
        if (!clock.isNight || agent.isAtCamp || agent.stamina > 0) return
        if (agent.mode == AgentMode.RETURNING || agent.returnHome) return
        kill(agent, reason = "stranded_night")
    }

    /**
     * Marks [agent] dead. Idempotent.
     */
    fun kill(agent: Agent, reason: String = "unspecified") {
        if (!agent.isAlive) return
        agent.mode = AgentMode.DEAD
        agent.path = emptyList()
        agent.returnHome = false
        // reason reserved for future logging / stats
        @Suppress("UNUSED_VARIABLE")
        val ignored = reason
    }

    /**
     * Advances [days] full days (`days * [SimConfig.HOURS_PER_DAY]` hours).
     */
    fun runDays(days: Int) {
        require(days >= 0) { "days must be >= 0 (got $days)" }
        runHours(days * SimConfig.HOURS_PER_DAY)
    }

    /** Advances exactly [hours] simulation hours. */
    fun runHours(hours: Int) {
        require(hours >= 0) { "hours must be >= 0 (got $hours)" }
        repeat(hours) { stepHour() }
    }

    /** Immutable UI / test projection of the current world. */
    fun snapshot(): SimSnapshot {
        val tiles = buildList {
            grid.forEachPeripheral { pos, tile ->
                add(
                    TileSnapshot(
                        x = pos.x,
                        y = pos.y,
                        state = tile.state,
                        ageDays = tile.ageDays,
                        pendingHarvest = tile.pendingHarvest,
                    ),
                )
            }
        }
        val agentSnaps = agents.map { agent ->
            AgentSnapshot(
                id = agent.id,
                gender = agent.gender,
                x = agent.pos.x,
                y = agent.pos.y,
                stamina = agent.stamina,
                carriedFood = agent.carriedFood,
                ageDays = agent.ageDays,
                mode = agent.mode,
                returnHome = agent.returnHome,
            )
        }
        return SimSnapshot(
            seed = seed,
            day = clock.day,
            hour = clock.hour,
            isDay = clock.isDay,
            isNight = clock.isNight,
            campFood = campFood,
            tiles = tiles,
            agents = agentSnaps,
            isGameOver = isGameOver,
        )
    }

    fun snapshotJson(): String = snapshotJsonFormat.encodeToString(snapshot())

    companion object {
        private val snapshotJsonFormat = Json {
            prettyPrint = false
            encodeDefaults = true
        }

        fun defaultAgents(): List<Agent> = listOf(
            Agent(id = "villager-m", gender = Gender.MALE),
            Agent(id = "villager-f", gender = Gender.FEMALE),
        )

        fun create(seed: Long): SimulationEngine = SimulationEngine(seed = seed)
    }
}
