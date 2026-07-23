package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.SimConfig

/**
 * Compressed in-game calendar for Tiny Camp.
 *
 * - One day has [SimConfig.HOURS_PER_DAY] hours, indexed `0 .. HOURS_PER_DAY-1`.
 * - Hours `0..2` are daytime; `3..5` are night (GDD §3.3).
 * - [advanceHour] wraps the hour counter and increments [day] at the boundary.
 *
 * @property day Zero-based day index (day 0 is the start of the simulation).
 * @property hour Current hour within the day.
 */
class Clock(
    var day: Int = 0,
    var hour: Int = 0,
) {
    init {
        require(day >= 0) { "day must be >= 0 (got $day)" }
        require(hour in 0 until SimConfig.HOURS_PER_DAY) {
            "hour must be in 0..${SimConfig.HOURS_PER_DAY - 1} (got $hour)"
        }
    }

    /** True during daytime hours (0, 1, 2). */
    val isDay: Boolean
        get() = hour < DAY_HOUR_COUNT

    /** True during night hours (3, 4, 5). */
    val isNight: Boolean
        get() = !isDay

    /**
     * Advances the clock by one hour.
     *
     * @return `true` if the advance crossed into a new day (`hour` reset to 0 and [day] incremented).
     */
    fun advanceHour(): Boolean {
        hour += 1
        if (hour >= SimConfig.HOURS_PER_DAY) {
            hour = 0
            day += 1
            return true
        }
        return false
    }

    companion object {
        /** Number of daytime hours at the start of each day. */
        const val DAY_HOUR_COUNT: Int = 3
    }
}
