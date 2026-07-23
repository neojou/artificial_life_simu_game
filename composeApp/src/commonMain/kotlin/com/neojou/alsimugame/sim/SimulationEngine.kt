package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.Agent
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.sim.model.TileSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Headless simulation driver for Tiny Camp.
 *
 * M1-T4 scope:
 * - Seeded [SimRng]
 * - Default world: 3×3 grass ring, camp food, two resting villagers at camp
 * - [stepHour] / [runDays] / [runHours] advance time + land
 * - Agents stay RESTING (no brain yet)
 * - [snapshot] for UI and determinism checks
 *
 * @property seed RNG seed that produced this world.
 * @property rng Seeded random source (for M2+ decisions).
 * @property grid World map.
 * @property clock In-game calendar.
 * @property campFood Global food at the homestead.
 * @property agents Mutable villager list (default male + female).
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

    init {
        require(campFood >= 0) { "campFood must be >= 0 (got $campFood)" }
    }

    val isGameOver: Boolean
        get() = agents.none { it.isAlive }

    /**
     * Advances one in-game hour.
     *
     * On day wrap (`hour` 5 → 0):
     * 1. Land ages and may transition.
     * 2. Each remaining FARM tile gains daily yield.
     * 3. Living agents age one day (lifespan enforcement is M2-T3).
     *
     * Agent AI actions are intentionally no-op (mode stays [AgentMode.RESTING]).
     */
    fun stepHour() {
        val crossedIntoNewDay = clock.advanceHour()
        if (crossedIntoNewDay) {
            LandSystem.ageAndTransition(grid)
            LandSystem.applyDailyFarmYield(grid)
            for (agent in agents) {
                if (agent.isAlive) {
                    agent.ageDays += 1
                }
            }
        }
        // M2: AgentBrain tick per living agent goes here.
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

    /**
     * Immutable UI / test projection of the current world.
     */
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

    /**
     * JSON encoding of [snapshot] (minimal save / share skeleton).
     */
    fun snapshotJson(): String = snapshotJsonFormat.encodeToString(snapshot())

    companion object {
        private val snapshotJsonFormat = Json {
            prettyPrint = false
            encodeDefaults = true
        }

        /** MVP starting pair: adult male + adult female at camp. */
        fun defaultAgents(): List<Agent> = listOf(
            Agent(id = "villager-m", gender = Gender.MALE),
            Agent(id = "villager-f", gender = Gender.FEMALE),
        )

        /**
         * Creates a fresh engine for [seed] with GDD initial conditions:
         * all grass, [SimConfig.INITIAL_CAMP_FOOD], two agents at camp.
         */
        fun create(seed: Long): SimulationEngine = SimulationEngine(seed = seed)
    }
}
