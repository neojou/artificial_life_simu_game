package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.sim.model.TileState
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulationEngineSeedTest {

    @Test
    fun create_initialWorld_matchesGddDefaults() {
        val engine = SimulationEngine.create(seed = 42L)
        val snap = engine.snapshot()

        assertEquals(42L, snap.seed)
        assertEquals(0, snap.day)
        assertEquals(0, snap.hour)
        assertTrue(snap.isDay)
        assertFalse(snap.isNight)
        assertEquals(SimConfig.INITIAL_CAMP_FOOD, snap.campFood)
        assertEquals(8, snap.tiles.size)
        assertEquals(8, snap.landStateCounts()[TileState.GRASS])
        assertEquals(0, snap.totalPendingHarvest())

        assertEquals(2, snap.agents.size)
        val genders = snap.agents.map { it.gender }.toSet()
        assertEquals(setOf(Gender.MALE, Gender.FEMALE), genders)
        for (agent in snap.agents) {
            assertEquals(GridPos.CAMP.x, agent.x)
            assertEquals(GridPos.CAMP.y, agent.y)
            assertEquals(SimConfig.MAX_STAMINA, agent.stamina)
            assertEquals(0, agent.carriedFood)
            assertEquals(0, agent.ageDays)
            assertEquals(AgentMode.RESTING, agent.mode)
        }
        assertFalse(snap.isGameOver)
        assertEquals(2, snap.livingAgentCount)
    }

    @Test
    fun sameSeed_runHours24_producesIdenticalSnapshots() {
        val seed = 2026L
        val a = SimulationEngine.create(seed)
        val b = SimulationEngine.create(seed)

        a.runHours(24)
        b.runHours(24)

        val sa = a.snapshot()
        val sb = b.snapshot()

        assertEquals(sa.day, sb.day)
        assertEquals(sa.hour, sb.hour)
        assertEquals(sa.campFood, sb.campFood)
        assertEquals(sa.landStateCounts(), sb.landStateCounts())
        assertEquals(sa.totalPendingHarvest(), sb.totalPendingHarvest())
        assertEquals(sa.tiles, sb.tiles)
        assertEquals(sa.agents, sb.agents)
        assertEquals(sa, sb)

        // 24 hours = 4 full days
        assertEquals(4, sa.day)
        assertEquals(0, sa.hour)
    }

    @Test
    fun differentSeeds_enginesRemainIndependent() {
        val a = SimulationEngine.create(1L)
        val b = SimulationEngine.create(2L)
        assertEquals(1L, a.seed)
        assertEquals(2L, b.seed)
        assertEquals(a.snapshot().campFood, b.snapshot().campFood)
        // RNG instances are distinct objects bound to their seed
        assertEquals(a.rng.seed, 1L)
        assertEquals(b.rng.seed, 2L)
    }

    @Test
    fun runDaysThroughLifespan_withAiDisabled_agesAgentsToDeath() {
        val engine = SimulationEngine.create(7L).also { it.aiEnabled = false }
        engine.runDays(SimConfig.LIFESPAN_DAYS)
        val snap = engine.snapshot()
        assertEquals(SimConfig.LIFESPAN_DAYS, snap.day)
        assertEquals(0, snap.hour)
        // Lifespan: ageDays reaches LIFESPAN_DAYS on that day boundary → DEAD
        for (agent in snap.agents) {
            assertEquals(SimConfig.LIFESPAN_DAYS, agent.ageDays)
            assertEquals(AgentMode.DEAD, agent.mode)
        }
        assertTrue(snap.isGameOver)
    }

    @Test
    fun snapshotJson_roundTrips() {
        val engine = SimulationEngine.create(99L)
        engine.runHours(6)
        val json = engine.snapshotJson()
        val decoded = Json.decodeFromString<SimSnapshot>(json)
        assertEquals(engine.snapshot(), decoded)
        assertEquals(1, decoded.day)
        assertEquals(0, decoded.hour)
    }

    @Test
    fun rng_isDeterministicForSameSeed() {
        val r1 = SimRng(123L)
        val r2 = SimRng(123L)
        val seq1 = List(10) { r1.nextInt(100) }
        val seq2 = List(10) { r2.nextInt(100) }
        assertEquals(seq1, seq2)
    }
}
