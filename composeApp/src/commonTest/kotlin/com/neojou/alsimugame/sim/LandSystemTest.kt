package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LandSystemTest {

    private val samplePos = GridPos(0, 0)

    @Test
    fun farmBecomesEmpty_afterLandStateDays() {
        val engine = SimulationEngine()
        val tile = engine.grid.tileAt(samplePos)
        assertNotNull(tile)
        LandSystem.till(tile)
        assertEquals(TileState.FARM, tile.state)
        assertEquals(0, tile.ageDays)

        engine.runDays(SimConfig.LAND_STATE_DAYS)

        assertEquals(TileState.EMPTY, tile.state)
        assertEquals(0, tile.ageDays)
        assertEquals(0, tile.pendingHarvest, "pending should clear on fallow")
    }

    @Test
    fun emptyBecomesGrass_afterLandStateDays() {
        val engine = SimulationEngine()
        val tile = engine.grid.tileAt(samplePos)
        assertNotNull(tile)
        tile.state = TileState.EMPTY
        tile.ageDays = 0
        tile.pendingHarvest = 0

        engine.runDays(SimConfig.LAND_STATE_DAYS)

        assertEquals(TileState.GRASS, tile.state)
        assertEquals(0, tile.ageDays)
    }

    @Test
    fun farmThenEmptyThenGrass_fullCycle_takesTwoLandStatePeriods() {
        val engine = SimulationEngine()
        val tile = engine.grid.tileAt(samplePos)
        assertNotNull(tile)
        LandSystem.till(tile)

        engine.runDays(SimConfig.LAND_STATE_DAYS)
        assertEquals(TileState.EMPTY, tile.state)

        engine.runDays(SimConfig.LAND_STATE_DAYS)
        assertEquals(TileState.GRASS, tile.state)
    }

    @Test
    fun threeNewDays_accumulatePendingHarvestOnFarm() {
        val engine = SimulationEngine()
        val tile = engine.grid.tileAt(samplePos)
        assertNotNull(tile)
        LandSystem.till(tile)
        assertEquals(0, tile.pendingHarvest)

        engine.runDays(3)

        assertEquals(TileState.FARM, tile.state)
        assertEquals(3 * SimConfig.FARM_YIELD_PER_DAY, tile.pendingHarvest)
        assertEquals(3, tile.ageDays)
    }

    @Test
    fun grassDoesNotGainPendingHarvest() {
        val engine = SimulationEngine()
        val tile = engine.grid.tileAt(samplePos)
        assertNotNull(tile)
        assertEquals(TileState.GRASS, tile.state)

        engine.runDays(3)

        assertEquals(0, tile.pendingHarvest)
        assertEquals(TileState.GRASS, tile.state)
    }

    @Test
    fun campHasNoTile_landSystemDoesNotCreateOne() {
        val engine = SimulationEngine()
        engine.runDays(1)
        assertEquals(null, engine.grid.tileAt(GridPos.CAMP))
    }

    @Test
    fun till_onlyAllowedOnGrass() {
        val tile = assertNotNull(Grid.createDefault().tileAt(samplePos))
        LandSystem.till(tile)
        assertEquals(TileState.FARM, tile.state)
        assertFailsWith<IllegalArgumentException> {
            LandSystem.till(tile)
        }
    }

    @Test
    fun ageAndTransition_directly_respectsThreshold() {
        val grid = Grid.createDefault()
        val tile = assertNotNull(grid.tileAt(samplePos))
        LandSystem.till(tile)

        repeat(SimConfig.LAND_STATE_DAYS - 1) {
            LandSystem.ageAndTransition(grid)
        }
        assertEquals(TileState.FARM, tile.state)
        assertEquals(SimConfig.LAND_STATE_DAYS - 1, tile.ageDays)

        LandSystem.ageAndTransition(grid)
        assertEquals(TileState.EMPTY, tile.state)
        assertEquals(0, tile.ageDays)
    }
}
