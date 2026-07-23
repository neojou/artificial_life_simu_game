package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveTest {

    @Test
    fun tryMove_toAdjacentPeripheral_spendsStamina() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        val startStamina = agent.stamina
        val dest = GridPos(0, 0)
        assertTrue(engine.tryMove(agent, dest))
        assertEquals(dest, agent.pos)
        assertEquals(startStamina - SimConfig.MOVE_STAMINA, agent.stamina)
    }

    @Test
    fun tryMove_failsWhenNotAdjacent() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        // from camp, (2,0) is adjacent actually... (0,0) is adjacent. (2,2) from camp is adjacent.
        // Place agent at (0,0) then try (2,2)
        agent.pos = GridPos(0, 0)
        assertFalse(engine.tryMove(agent, GridPos(2, 2)))
        assertEquals(GridPos(0, 0), agent.pos)
    }

    @Test
    fun tryMove_failsWhenOutOfBounds() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        assertFalse(engine.tryMove(agent, GridPos(-1, 0)))
        assertEquals(GridPos.CAMP, agent.pos)
    }

    @Test
    fun tryMove_failsWhenInsufficientStamina_withoutForce() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        agent.stamina = 0
        assertFalse(engine.tryMove(agent, GridPos(0, 0), force = false))
        assertEquals(GridPos.CAMP, agent.pos)
        assertEquals(0, agent.stamina)
    }

    @Test
    fun tryMove_force_movesEvenAtZeroStamina() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        agent.stamina = 0
        assertTrue(engine.tryMove(agent, GridPos(0, 1), force = true))
        assertEquals(GridPos(0, 1), agent.pos)
        assertEquals(0, agent.stamina)
    }

    @Test
    fun tryMove_failsWhenDead() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        agent.mode = AgentMode.DEAD
        assertFalse(engine.tryMove(agent, GridPos(0, 0)))
    }
}
