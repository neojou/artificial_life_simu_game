package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.Tile
import com.neojou.alsimugame.sim.model.TileState

/**
 * Fixed [SimConfig.GRID_SIZE]×[SimConfig.GRID_SIZE] world map.
 *
 * Layout:
 * - Center [GridPos.CAMP] is the non-tillable homestead (no [Tile]).
 * - All other cells are peripheral land, each holding a [Tile].
 *
 * Construction initialises every peripheral tile to [TileState.GRASS]
 * with zero age and zero pending harvest (GDD MVP defaults).
 */
class Grid(
    val size: Int = SimConfig.GRID_SIZE,
) {
    init {
        require(size >= 3 && size % 2 == 1) {
            "Grid size must be an odd integer >= 3 (got $size)"
        }
        require(GridPos.CAMP.isInBounds(size)) {
            "Camp ${GridPos.CAMP} is out of bounds for size $size"
        }
    }

    /**
     * Row-major storage for every cell; camp slot is always `null`.
     * Index: `y * size + x`.
     */
    private val cells: Array<Tile?> = Array(size * size) { index ->
        val x = index % size
        val y = index / size
        val pos = GridPos(x, y)
        if (pos.isCamp()) null else Tile()
    }

    /** Camp / home-base coordinate for this map. */
    val camp: GridPos get() = GridPos.CAMP

    /** Number of peripheral (tillable) cells. */
    val peripheralCount: Int get() = size * size - 1

    fun isInBounds(pos: GridPos): Boolean = pos.isInBounds(size)

    fun isCamp(pos: GridPos): Boolean = pos.isCamp()

    /**
     * Returns the land [Tile] at [pos], or `null` if out of bounds or camp.
     */
    fun tileAt(pos: GridPos): Tile? {
        if (!isInBounds(pos) || isCamp(pos)) return null
        return cells[indexOf(pos)]
    }

    /**
     * Whether [pos] is a peripheral cell that can be tilled / farmed.
     */
    fun isTillable(pos: GridPos): Boolean = tileAt(pos) != null

    /**
     * All in-bounds coordinates in row-major order (includes camp).
     */
    fun allPositions(): List<GridPos> {
        val n = size
        return buildList(n * n) {
            for (y in 0 until n) {
                for (x in 0 until n) {
                    add(GridPos(x, y))
                }
            }
        }
    }

    /**
     * All peripheral (non-camp) coordinates in row-major order.
     */
    fun peripheralPositions(): List<GridPos> =
        allPositions().filterNot { it.isCamp() }

    /**
     * Invokes [action] for each peripheral tile.
     */
    fun forEachPeripheral(action: (pos: GridPos, tile: Tile) -> Unit) {
        for (pos in peripheralPositions()) {
            val tile = tileAt(pos) ?: continue
            action(pos, tile)
        }
    }

    /**
     * Snapshot of land states for assertions / UI (camp omitted).
     */
    fun peripheralTiles(): Map<GridPos, Tile> = buildMap {
        forEachPeripheral { pos, tile -> put(pos, tile) }
    }

    private fun indexOf(pos: GridPos): Int = pos.y * size + pos.x

    companion object {
        /** Creates a default MVP grid: [SimConfig.GRID_SIZE]², camp center, all grass. */
        fun createDefault(): Grid = Grid(SimConfig.GRID_SIZE)
    }
}
