package com.neojou.alsimugame.sim.path

import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.math.abs
import kotlin.math.max

/**
 * Grid navigation helpers for the tiny 3×3 map (8-direction movement).
 *
 * Distance metric is Chebyshev (diagonal counts as 1), matching GDD §5.2.
 */
object Pathfinder {

    /** Eight direction offsets: N, NE, E, SE, S, SW, W, NW. */
    val DIRECTIONS_8: List<GridPos> = listOf(
        GridPos(0, -1),
        GridPos(1, -1),
        GridPos(1, 0),
        GridPos(1, 1),
        GridPos(0, 1),
        GridPos(-1, 1),
        GridPos(-1, 0),
        GridPos(-1, -1),
    )

    /** Chebyshev distance (max of |dx|, |dy|). */
    fun chebyshev(a: GridPos, b: GridPos): Int =
        max(abs(a.x - b.x), abs(a.y - b.y))

    /** True if [a] and [b] are distinct and within one 8-directional step. */
    fun areAdjacent(a: GridPos, b: GridPos): Boolean =
        a != b && chebyshev(a, b) == 1

    /**
     * In-bounds 8-neighbours of [pos].
     */
    fun neighbors(pos: GridPos, size: Int = SimConfig.GRID_SIZE): List<GridPos> =
        DIRECTIONS_8.mapNotNull { d ->
            val n = GridPos(pos.x + d.x, pos.y + d.y)
            if (n.isInBounds(size)) n else null
        }

    /**
     * Shortest path from [from] to [to] as successive cells **excluding** [from],
     * **including** [to]. Empty if already there; empty if unreachable (should not
     * happen on a fully walkable square grid).
     */
    fun findPath(
        from: GridPos,
        to: GridPos,
        size: Int = SimConfig.GRID_SIZE,
    ): List<GridPos> {
        if (!from.isInBounds(size) || !to.isInBounds(size)) return emptyList()
        if (from == to) return emptyList()

        val queue = ArrayDeque<GridPos>()
        val cameFrom = mutableMapOf<GridPos, GridPos?>()
        queue.add(from)
        cameFrom[from] = null

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == to) break
            for (n in neighbors(current, size)) {
                if (n in cameFrom) continue
                cameFrom[n] = current
                queue.add(n)
            }
        }

        if (to !in cameFrom) return emptyList()

        val path = ArrayList<GridPos>()
        var cursor: GridPos? = to
        while (cursor != null && cursor != from) {
            path.add(cursor)
            cursor = cameFrom[cursor]
        }
        path.reverse()
        return path
    }

    /** Path from [from] to the camp cell. */
    fun pathToCamp(from: GridPos, size: Int = SimConfig.GRID_SIZE): List<GridPos> =
        findPath(from, GridPos.CAMP, size)

    /**
     * First step along the shortest path toward [to], or `null` if already there
     * or unreachable.
     */
    fun nextStepToward(
        from: GridPos,
        to: GridPos,
        size: Int = SimConfig.GRID_SIZE,
    ): GridPos? = findPath(from, to, size).firstOrNull()

    fun nextStepTowardCamp(
        from: GridPos,
        size: Int = SimConfig.GRID_SIZE,
    ): GridPos? = nextStepToward(from, GridPos.CAMP, size)
}
