package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.path.Pathfinder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveTest {

    /** A cell adjacent to camp (not corner on 5×5). */
    private val adjacentToCamp = GridPos(SimConfig.CAMP_X - 1, SimConfig.CAMP_Y)

    @Test
    fun tryMove_toAdjacentPeripheral_spendsStamina() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        val startStamina = agent.stamina
        assertTrue(Pathfinder.areAdjacent(GridPos.CAMP, adjacentToCamp))
        assertTrue(engine.tryMove(agent, adjacentToCamp))
        assertEquals(adjacentToCamp, agent.pos)
        assertEquals(startStamina - SimConfig.MOVE_STAMINA, agent.stamina)
    }

    @Test
    fun tryMove_failsWhenNotAdjacent() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        agent.pos = GridPos(0, 0)
        // Far jump across the map
        assertFalse(engine.tryMove(agent, GridPos(4, 4)))
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
        assertFalse(engine.tryMove(agent, adjacentToCamp, force = false))
        assertEquals(GridPos.CAMP, agent.pos)
        assertEquals(0, agent.stamina)
    }

    @Test
    fun tryMove_force_movesEvenAtZeroStamina() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        agent.stamina = 0
        assertTrue(engine.tryMove(agent, adjacentToCamp, force = true))
        assertEquals(adjacentToCamp, agent.pos)
        assertEquals(0, agent.stamina)
    }

    @Test
    fun tryMove_failsWhenDead() {
        val engine = SimulationEngine.create(1L)
        val agent = engine.agents.first()
        agent.mode = AgentMode.DEAD
        assertFalse(engine.tryMove(agent, adjacentToCamp))
    }
}
