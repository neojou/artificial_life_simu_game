package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsRecorderTest {

    @Test
    fun recordYieldHarvestDeposit_accumulate() {
        val s = StatsRecorder()
        s.recordYield(2)
        s.recordYield(1)
        s.recordHarvest(3)
        s.recordDeposit(3)
        assertEquals(3, s.totalFoodProduced)
        assertEquals(3, s.totalFoodHarvested)
        assertEquals(3, s.totalFoodDeposited)
    }

    @Test
    fun engine_dailyYield_updatesStatsAndSnapshot() {
        val engine = SimulationEngine.create(1L).also { it.aiEnabled = false }
        val tile = engine.grid.tileAt(GridPos(0, 0))!!
        LandSystem.till(tile)
        assertEquals(0, engine.stats.totalFoodProduced)

        engine.runDays(2)
        assertEquals(2, engine.stats.totalFoodProduced)
        assertEquals(2, engine.snapshot().totalFoodProduced)
        assertEquals(2, tile.pendingHarvest)
    }

    @Test
    fun engine_harvest_updatesHarvestedStat() {
        val engine = SimulationEngine.create(2L).also { it.aiEnabled = false }
        val tile = engine.grid.tileAt(GridPos(0, 0))!!
        LandSystem.till(tile)
        engine.runDays(3)
        val agent = engine.agents.first()
        agent.pos = GridPos(0, 0)
        val amount = Economy.harvest(agent, tile)
        engine.stats.recordHarvest(amount)
        assertEquals(3, amount)
        assertEquals(3, engine.stats.totalFoodHarvested)
        assertEquals(0, tile.pendingHarvest)
        assertTrue(engine.snapshot().totalFoodProduced >= 3)
    }

    @Test
    fun landStateCounts_matchGrid() {
        val engine = SimulationEngine.create(0L).also { it.aiEnabled = false }
        val snap = engine.snapshot()
        assertEquals(24, snap.landStateCounts()[TileState.GRASS])
        assertEquals(0, snap.totalFoodProduced)
    }
}
