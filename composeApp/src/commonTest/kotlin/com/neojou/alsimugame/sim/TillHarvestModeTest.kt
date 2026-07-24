package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression: work modes must remain visible for the full hour after till/harvest
 * (UI badges 墾/收). returnHome is set immediately; RETURNING starts next hour.
 */
class TillHarvestModeTest {

    @Test
    fun till_keepsTillingMode_forThatHour_thenReturningNextHour() {
        val engine = SimulationEngine.create(seed = 7L)
        // Only one actor so AI outcomes are deterministic for that agent.
        engine.agents[1].mode = AgentMode.DEAD
        val agent = engine.agents[0]
        agent.pos = GridPos(0, 0)
        agent.stamina = SimConfig.MAX_STAMINA
        agent.mode = AgentMode.EXPLORING
        agent.returnHome = false
        agent.carriedFood = 0

        assertEquals(TileState.GRASS, engine.grid.tileAt(agent.pos)!!.state)
        assertTrue(engine.clock.isDay, "need daytime to till")

        engine.stepHour()

        assertEquals(TileState.FARM, engine.grid.tileAt(GridPos(0, 0))!!.state)
        assertEquals(AgentMode.TILLING, agent.mode, "mode must stay TILLING this hour for UI")
        assertTrue(agent.returnHome, "after work, plan to return next hours")

        engine.stepHour()

        assertEquals(
            AgentMode.RETURNING,
            agent.mode,
            "next hour should switch to RETURNING and walk home",
        )
        assertTrue(agent.returnHome || agent.isAtCamp)
    }

    @Test
    fun harvest_keepsHarvestingMode_forThatHour_thenReturningNextHour() {
        val engine = SimulationEngine.create(seed = 11L)
        engine.agents[1].mode = AgentMode.DEAD
        val agent = engine.agents[0]
        val pos = GridPos(0, 0)
        agent.pos = pos
        agent.stamina = SimConfig.MAX_STAMINA
        agent.mode = AgentMode.EXPLORING
        agent.returnHome = false
        agent.carriedFood = 0

        val tile = engine.grid.tileAt(pos)!!
        tile.state = TileState.FARM
        tile.ageDays = 0
        tile.pendingHarvest = 2

        assertTrue(engine.clock.isDay)

        engine.stepHour()

        assertEquals(0, tile.pendingHarvest)
        assertEquals(2, agent.carriedFood)
        assertEquals(AgentMode.HARVESTING, agent.mode, "mode must stay HARVESTING this hour for UI")
        assertTrue(agent.returnHome)

        engine.stepHour()

        assertEquals(AgentMode.RETURNING, agent.mode)
    }
}
