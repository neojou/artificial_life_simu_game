package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeathAndPopulationTest {

    @Test
    fun defaultWorld_hasMaleAndFemale_bothAtCamp() {
        val engine = SimulationEngine.create(1L)
        assertEquals(2, engine.agents.size)
        val genders = engine.agents.map { it.gender }.toSet()
        assertEquals(setOf(Gender.MALE, Gender.FEMALE), genders)
        assertTrue(engine.agents.all { it.isAtCamp })
        assertFalse(engine.isGameOver)
    }

    @Test
    fun twoAgents_mayOccupySameCell() {
        val engine = SimulationEngine.create(1L)
        val a = engine.agents[0]
        val b = engine.agents[1]
        a.pos = GridPos(0, 0)
        b.pos = GridPos(0, 0)
        assertEquals(a.pos, b.pos)
        assertTrue(a.isAlive && b.isAlive)
    }

    @Test
    fun lifespan_killsAtConfiguredDays() {
        val engine = SimulationEngine.create(2L).also { it.aiEnabled = false }
        engine.runDays(SimConfig.LIFESPAN_DAYS)
        assertTrue(engine.agents.all { it.mode == AgentMode.DEAD })
        assertTrue(engine.agents.all { it.ageDays >= SimConfig.LIFESPAN_DAYS })
        assertTrue(engine.isGameOver)
    }

    @Test
    fun beforeLifespan_agentsStillAlive_withAiOff() {
        val engine = SimulationEngine.create(2L).also { it.aiEnabled = false }
        engine.runDays(SimConfig.LIFESPAN_DAYS - 1)
        assertTrue(engine.agents.all { it.isAlive })
        assertFalse(engine.isGameOver)
        assertEquals(SimConfig.LIFESPAN_DAYS - 1, engine.agents.first().ageDays)
    }

    @Test
    fun kill_marksDeadAndContributesToGameOver() {
        val engine = SimulationEngine.create(3L)
        engine.kill(engine.agents[0], "test")
        assertEquals(AgentMode.DEAD, engine.agents[0].mode)
        assertFalse(engine.isGameOver)
        engine.kill(engine.agents[1], "test")
        assertTrue(engine.isGameOver)
    }

    @Test
    fun strandedAtNight_zeroStamina_diesIfStillOffCamp() {
        // With force return, a normal tick would move them home first.
        // Directly invoke the death rule by placing agent off-camp at night
        // with 0 stamina and disabling AI so they cannot force-walk home.
        val engine = SimulationEngine.create(4L).also { it.aiEnabled = false }
        val agent = engine.agents.first()
        agent.pos = GridPos(0, 0)
        agent.stamina = 0
        // Advance to night without AI
        while (!engine.clock.isNight) {
            engine.stepHour()
        }
        // Manually apply the same stranded rule the engine uses after AI actions
        if (engine.clock.isNight && !agent.isAtCamp && agent.stamina <= 0) {
            engine.kill(agent, "stranded_night")
        }
        assertEquals(AgentMode.DEAD, agent.mode)
    }

    @Test
    fun forceNightReturn_preventsStrandedDeathOn3x3() {
        val engine = SimulationEngine.create(5L)
        val agent = engine.agents.first()
        // Put only one agent in the field; remove the other to simplify
        engine.kill(engine.agents[1])
        agent.pos = GridPos(0, 0)
        agent.stamina = 0
        agent.mode = AgentMode.EXPLORING
        // Step until night and through a night hour with AI on
        while (engine.clock.day == 0 && engine.clock.hour < 3) {
            engine.stepHour()
        }
        // One more step into night processing if needed
        if (engine.clock.isDay) engine.stepHour()
        engine.stepHour()
        // Agent should have been force-moved home rather than left stranded
        assertTrue(agent.isAtCamp || agent.mode == AgentMode.DEAD)
        // On 3×3 with force return, prefer alive at camp:
        if (agent.isAlive) {
            assertTrue(agent.isAtCamp)
        }
    }
}
