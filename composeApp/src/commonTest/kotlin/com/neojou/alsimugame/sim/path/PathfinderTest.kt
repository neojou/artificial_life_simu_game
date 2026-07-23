package com.neojou.alsimugame.sim.path

import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathfinderTest {

    @Test
    fun eightDirections_defined() {
        assertEquals(8, Pathfinder.DIRECTIONS_8.size)
    }

    @Test
    fun neighbors_centerHasEight_cornerHasThree() {
        assertEquals(8, Pathfinder.neighbors(GridPos.CAMP).size)
        assertEquals(3, Pathfinder.neighbors(GridPos(0, 0)).size)
        assertEquals(5, Pathfinder.neighbors(GridPos(1, 0)).size)
    }

    @Test
    fun areAdjacent_eightWayIncludingDiagonal() {
        assertTrue(Pathfinder.areAdjacent(GridPos.CAMP, GridPos(0, 0)))
        assertTrue(Pathfinder.areAdjacent(GridPos.CAMP, GridPos(1, 0)))
        assertFalse(Pathfinder.areAdjacent(GridPos(0, 0), GridPos(2, 2)))
        assertFalse(Pathfinder.areAdjacent(GridPos.CAMP, GridPos.CAMP))
    }

    @Test
    fun pathFromAnyPeripheralToCamp_isExactlyOneStep() {
        for (x in 0 until SimConfig.GRID_SIZE) {
            for (y in 0 until SimConfig.GRID_SIZE) {
                val pos = GridPos(x, y)
                if (pos.isCamp()) continue
                val path = Pathfinder.pathToCamp(pos)
                assertEquals(1, path.size, "from $pos")
                assertEquals(GridPos.CAMP, path.single())
                assertEquals(GridPos.CAMP, Pathfinder.nextStepTowardCamp(pos))
            }
        }
    }

    @Test
    fun pathToSelf_isEmpty() {
        assertTrue(Pathfinder.findPath(GridPos.CAMP, GridPos.CAMP).isEmpty())
        assertNull(Pathfinder.nextStepTowardCamp(GridPos.CAMP))
    }

    @Test
    fun pathBetweenCorners_usesShortestChebyshev() {
        val path = Pathfinder.findPath(GridPos(0, 0), GridPos(2, 2))
        assertEquals(2, path.size)
        assertEquals(GridPos(2, 2), path.last())
        // each step must be adjacent
        var prev = GridPos(0, 0)
        for (step in path) {
            assertTrue(Pathfinder.areAdjacent(prev, step), "$prev -> $step")
            prev = step
        }
    }

    @Test
    fun outOfBounds_yieldsEmptyPath() {
        assertTrue(Pathfinder.findPath(GridPos(0, 0), GridPos(9, 9)).isEmpty())
    }
}
