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
        // Camp (2,2) is adjacent to (1,1) and (2,1), not to corner (0,0) on 5×5
        assertTrue(Pathfinder.areAdjacent(GridPos.CAMP, GridPos(1, 1)))
        assertTrue(Pathfinder.areAdjacent(GridPos.CAMP, GridPos(2, 1)))
        assertFalse(Pathfinder.areAdjacent(GridPos.CAMP, GridPos(0, 0)))
        assertFalse(Pathfinder.areAdjacent(GridPos(0, 0), GridPos(2, 2)))
        assertFalse(Pathfinder.areAdjacent(GridPos.CAMP, GridPos.CAMP))
    }

    @Test
    fun pathToCamp_lengthEqualsChebyshevDistance() {
        for (x in 0 until SimConfig.GRID_SIZE) {
            for (y in 0 until SimConfig.GRID_SIZE) {
                val pos = GridPos(x, y)
                if (pos.isCamp()) continue
                val path = Pathfinder.pathToCamp(pos)
                val dist = Pathfinder.chebyshev(pos, GridPos.CAMP)
                assertEquals(dist, path.size, "from $pos")
                assertEquals(GridPos.CAMP, path.last())
                val step = Pathfinder.nextStepTowardCamp(pos)
                assertTrue(step != null && Pathfinder.areAdjacent(pos, step))
            }
        }
    }

    @Test
    fun cornerToCamp_isTwoSteps_on5x5() {
        val path = Pathfinder.pathToCamp(GridPos(0, 0))
        assertEquals(2, path.size)
        assertEquals(GridPos.CAMP, path.last())
    }

    @Test
    fun pathToSelf_isEmpty() {
        assertTrue(Pathfinder.findPath(GridPos.CAMP, GridPos.CAMP).isEmpty())
        assertNull(Pathfinder.nextStepTowardCamp(GridPos.CAMP))
    }

    @Test
    fun pathBetweenOppositeCorners_usesShortestChebyshev() {
        val from = GridPos(0, 0)
        val to = GridPos(SimConfig.GRID_SIZE - 1, SimConfig.GRID_SIZE - 1)
        val path = Pathfinder.findPath(from, to)
        assertEquals(Pathfinder.chebyshev(from, to), path.size)
        assertEquals(to, path.last())
        var prev = from
        for (step in path) {
            assertTrue(Pathfinder.areAdjacent(prev, step), "$prev -> $step")
            prev = step
        }
    }

    @Test
    fun outOfBounds_yieldsEmptyPath() {
        assertTrue(Pathfinder.findPath(GridPos(0, 0), GridPos(99, 99)).isEmpty())
    }
}
