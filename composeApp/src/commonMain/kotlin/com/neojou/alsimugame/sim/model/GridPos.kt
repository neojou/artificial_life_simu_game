package com.neojou.alsimugame.sim.model

/**
 * Integer cell coordinate on the square simulation map.
 *
 * Valid range for the default map: `x,y ∈ [0, [SimConfig.GRID_SIZE])`.
 * The camp home base is always [CAMP] = `(1, 1)`.
 *
 * @property x Column index (0 = west).
 * @property y Row index (0 = north).
 */
data class GridPos(
    val x: Int,
    val y: Int,
) {
    /** True if this position lies inside a [size]×[size] map. */
    fun isInBounds(size: Int = SimConfig.GRID_SIZE): Boolean =
        x in 0 until size && y in 0 until size

    /** True if this cell is the non-tillable camp / home base. */
    fun isCamp(): Boolean = this == CAMP

    companion object {
        /** Home base at the center of the default 3×3 map. */
        val CAMP: GridPos = GridPos(SimConfig.CAMP_X, SimConfig.CAMP_Y)
    }
}
