package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.Agent
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentModelTest {

    @Test
    fun defaultAgent_startsAtCampWithFullStamina() {
        val agent = Agent(id = "villager-m", gender = Gender.MALE)
        assertEquals(GridPos.CAMP, agent.pos)
        assertTrue(agent.isAtCamp)
        assertEquals(SimConfig.MAX_STAMINA, agent.stamina)
        assertEquals(0, agent.carriedFood)
        assertEquals(0, agent.ageDays)
        assertEquals(AgentMode.RESTING, agent.mode)
        assertTrue(agent.isAlive)
        assertFalse(agent.returnHome)
        assertTrue(agent.path.isEmpty())
    }

    @Test
    fun deadMode_isNotAlive() {
        val agent = Agent(
            id = "villager-f",
            gender = Gender.FEMALE,
            mode = AgentMode.DEAD,
        )
        assertFalse(agent.isAlive)
    }
}
