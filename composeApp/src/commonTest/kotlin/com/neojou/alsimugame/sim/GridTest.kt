package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GridTest {

    @Test
    fun campIsAtMapCenter() {
        val grid = Grid.createDefault()
        val expected = GridPos(SimConfig.CAMP_X, SimConfig.CAMP_Y)
        assertEquals(expected, grid.camp)
        assertEquals(GridPos.CAMP, grid.camp)
        assertTrue(grid.isCamp(expected))
        assertFalse(grid.isCamp(GridPos(0, 0)))
    }

    @Test
    fun gridIs5x5_withTwentyFiveCellsAndTwentyFourPeripheral() {
        val grid = Grid.createDefault()
        assertEquals(5, grid.size)
        assertEquals(SimConfig.GRID_SIZE, grid.size)
        assertEquals(25, grid.allPositions().size)
        assertEquals(24, grid.peripheralCount)
        assertEquals(24, grid.peripheralPositions().size)
    }

    @Test
    fun allCoordinatesInBounds_0_to_sizeMinusOne() {
        val grid = Grid.createDefault()
        val max = SimConfig.GRID_SIZE - 1
        for (pos in grid.allPositions()) {
            assertTrue(pos.isInBounds(), "expected in-bounds: $pos")
            assertTrue(grid.isInBounds(pos))
            assertTrue(pos.x in 0..max)
            assertTrue(pos.y in 0..max)
        }
        assertFalse(GridPos(-1, 0).isInBounds())
        assertFalse(GridPos(SimConfig.GRID_SIZE, 1).isInBounds())
        assertFalse(grid.isInBounds(GridPos(0, SimConfig.GRID_SIZE)))
    }

    @Test
    fun campIsNotTillable_andHasNoTile() {
        val grid = Grid.createDefault()
        val camp = GridPos.CAMP
        assertNull(grid.tileAt(camp), "camp must not expose a land Tile")
        assertFalse(grid.isTillable(camp))
        assertTrue(grid.isCamp(camp))
    }

    @Test
    fun peripheralTilesStartAsGrass_withZeroPendingAndAge() {
        val grid = Grid.createDefault()
        var count = 0
        grid.forEachPeripheral { pos, tile ->
            count++
            assertFalse(pos.isCamp(), "peripheral listed camp: $pos")
            assertEquals(TileState.GRASS, tile.state, "pos=$pos")
            assertEquals(0, tile.ageDays, "pos=$pos")
            assertEquals(0, tile.pendingHarvest, "pos=$pos")
            assertFalse(tile.hasHarvest)
            assertNotNull(grid.tileAt(pos))
            assertTrue(grid.isTillable(pos))
        }
        assertEquals(24, count)
    }

    @Test
    fun outOfBoundsTileAt_returnsNull() {
        val grid = Grid.createDefault()
        assertNull(grid.tileAt(GridPos(-1, 0)))
        assertNull(grid.tileAt(GridPos(SimConfig.GRID_SIZE, SimConfig.GRID_SIZE)))
    }
}
