package com.neojou.alsimugame.sim

import kotlin.random.Random

/**
 * Seeded random source for the simulation.
 *
 * All stochastic agent decisions (M2+) must go through this wrapper so that
 * identical [seed] + identical [SimulationEngine.stepHour] counts reproduce
 * the same world state (Architecture §9).
 *
 * @property seed Value used to initialise the underlying [Random].
 */
class SimRng(
    val seed: Long,
) {
    private val random: Random = Random(seed)

    fun nextInt(until: Int): Int = random.nextInt(until)

    fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)

    fun nextLong(): Long = random.nextLong()

    fun nextBoolean(): Boolean = random.nextBoolean()

    /** Uniform pick from a non-empty list. */
    fun <T> nextFrom(items: List<T>): T {
        require(items.isNotEmpty()) { "Cannot pick from an empty list" }
        return items[nextInt(items.size)]
    }
}
