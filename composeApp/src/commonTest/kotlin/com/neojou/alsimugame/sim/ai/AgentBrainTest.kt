package com.neojou.alsimugame.sim.ai

import com.neojou.alsimugame.sim.Clock
import com.neojou.alsimugame.sim.Economy
import com.neojou.alsimugame.sim.Grid
import com.neojou.alsimugame.sim.LandSystem
import com.neojou.alsimugame.sim.SimRng
import com.neojou.alsimugame.sim.SimulationEngine
import com.neojou.alsimugame.sim.model.Agent
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AgentBrainTest {

    private fun world(
        hour: Int = 0,
        day: Int = 0,
        seed: Long = 1L,
        grid: Grid = Grid.createDefault(),
    ): BrainWorld = BrainWorld(
        grid = grid,
        clock = Clock(day = day, hour = hour),
        rng = SimRng(seed),
    )

    private fun agent(
        pos: GridPos = GridPos.CAMP,
        stamina: Int = 10,
        mode: AgentMode = AgentMode.RESTING,
        returnHome: Boolean = false,
    ): Agent = Agent(
        id = "a",
        gender = Gender.MALE,
        pos = pos,
        stamina = stamina,
        mode = mode,
        returnHome = returnHome,
    )

    @Test
    fun dead_returnsNone() {
        val a = agent(mode = AgentMode.DEAD)
        assertEquals(AgentAction.None, AgentBrain.decide(a, world()))
    }

    @Test
    fun nightAwayFromCamp_forceMoveTowardCamp() {
        val a = agent(pos = GridPos(0, 0), stamina = 0)
        val action = AgentBrain.decide(a, world(hour = 4))
        val move = assertIs<AgentAction.Move>(action)
        assertEquals(GridPos.CAMP, move.dest)
        assertTrue(move.force)
        assertEquals(AgentMode.RETURNING, a.mode)
    }

    @Test
    fun lowStaminaAway_returnsHome() {
        val a = agent(pos = GridPos(2, 2), stamina = 1)
        val action = AgentBrain.decide(a, world(hour = 1))
        val move = assertIs<AgentAction.Move>(action)
        assertEquals(GridPos.CAMP, move.dest)
        assertTrue(a.returnHome)
    }

    @Test
    fun returnHomeAtCamp_supplies() {
        val a = agent(pos = GridPos.CAMP, returnHome = true, mode = AgentMode.RETURNING)
        assertEquals(AgentAction.SupplyAtCamp, AgentBrain.decide(a, world(hour = 1)))
    }

    @Test
    fun dayAtCampWithStamina_startsExploreMove() {
        val a = agent(pos = GridPos.CAMP, stamina = 5)
        val action = AgentBrain.decide(a, world(hour = 0, seed = 0L))
        val move = assertIs<AgentAction.Move>(action)
        assertFalse(move.dest.isCamp())
        assertEquals(AgentMode.EXPLORING, a.mode)
    }

    @Test
    fun onGrass_withStamina_tills() {
        val a = agent(pos = GridPos(0, 0), stamina = 5, mode = AgentMode.EXPLORING)
        val action = AgentBrain.decide(a, world(hour = 1))
        assertEquals(AgentAction.TillHere, action)
    }

    @Test
    fun onFarmWithPending_harvests() {
        val grid = Grid.createDefault()
        val tile = grid.tileAt(GridPos(0, 0))!!
        LandSystem.till(tile)
        tile.pendingHarvest = 3
        val a = agent(pos = GridPos(0, 0), stamina = 5, mode = AgentMode.EXPLORING)
        val action = AgentBrain.decide(a, world(hour = 1, grid = grid))
        assertEquals(AgentAction.HarvestHere, action)
    }

    @Test
    fun onEmptyFarm_continuesExploreWithoutReturnFlag() {
        val grid = Grid.createDefault()
        val tile = grid.tileAt(GridPos(0, 0))!!
        LandSystem.till(tile)
        tile.pendingHarvest = 0
        val a = agent(pos = GridPos(0, 0), stamina = 5, mode = AgentMode.EXPLORING)
        val action = AgentBrain.decide(a, world(hour = 1, grid = grid, seed = 3L))
        assertIs<AgentAction.Move>(action)
        assertFalse(a.returnHome)
    }

    @Test
    fun tillAction_setsReturnHomeWhenExecutedViaEconomy() {
        val agent = agent(pos = GridPos(0, 0), stamina = 5)
        val tile = Grid.createDefault().tileAt(GridPos(0, 0))!!
        assertTrue(Economy.tryPayAndTill(agent, tile))
        assertEquals(TileState.FARM, tile.state)
        agent.returnHome = true
        agent.mode = AgentMode.RETURNING
        val back = AgentBrain.decide(agent, world(hour = 1))
        val move = assertIs<AgentAction.Move>(back)
        assertEquals(GridPos.CAMP, move.dest)
    }
}

class AgentBrainIntegrationTest {

    @Test
    fun runDays3_someSeed_leavesCampOrTillsLand() {
        var observed = false
        for (seed in 0L..50L) {
            val engine = SimulationEngine.create(seed)
            var sawOffCamp = false
            var sawFarm = false
            repeat(3 * 6) {
                engine.stepHour()
                if (engine.agents.any { it.isAlive && !it.isAtCamp }) sawOffCamp = true
                if (engine.grid.peripheralTiles().values.any { it.state == TileState.FARM }) {
                    sawFarm = true
                }
            }
            if (sawOffCamp || sawFarm) {
                observed = true
                break
            }
        }
        assertTrue(
            observed,
            "expected some seed in 0..50 to leave camp or till land within 3 days",
        )
    }

    @Test
    fun sameSeed_aiRunIsDeterministic() {
        fun run(): String {
            val e = SimulationEngine.create(99L)
            e.runDays(5)
            return e.snapshotJson()
        }
        assertEquals(run(), run())
    }
}
