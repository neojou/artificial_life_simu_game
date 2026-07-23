package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClockTest {

    @Test
    fun initialClock_isDayZeroHourZero_daytime() {
        val clock = Clock()
        assertEquals(0, clock.day)
        assertEquals(0, clock.hour)
        assertTrue(clock.isDay)
        assertFalse(clock.isNight)
    }

    @Test
    fun hoursZeroToTwo_areDay_threeToFive_areNight() {
        for (h in 0..2) {
            val clock = Clock(day = 0, hour = h)
            assertTrue(clock.isDay, "hour $h should be day")
            assertFalse(clock.isNight, "hour $h should not be night")
        }
        for (h in 3..5) {
            val clock = Clock(day = 0, hour = h)
            assertTrue(clock.isNight, "hour $h should be night")
            assertFalse(clock.isDay, "hour $h should not be day")
        }
    }

    @Test
    fun advanceHour_cyclesZeroThroughFive_thenWrapsToNewDay() {
        val clock = Clock()
        val newDayFlags = mutableListOf<Boolean>()
        val hours = mutableListOf<Int>()
        val days = mutableListOf<Int>()

        repeat(SimConfig.HOURS_PER_DAY + 1) {
            val wrapped = clock.advanceHour()
            newDayFlags += wrapped
            hours += clock.hour
            days += clock.day
        }

        // After 1..5 advances: hour 1,2,3,4,5 — no wrap
        assertEquals(listOf(false, false, false, false, false, true, false), newDayFlags)
        assertEquals(listOf(1, 2, 3, 4, 5, 0, 1), hours)
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 1), days)
    }

    @Test
    fun sixAdvances_makeExactlyOneDay() {
        val clock = Clock()
        var wraps = 0
        repeat(SimConfig.HOURS_PER_DAY) {
            if (clock.advanceHour()) wraps++
        }
        assertEquals(1, wraps)
        assertEquals(1, clock.day)
        assertEquals(0, clock.hour)
    }
}
