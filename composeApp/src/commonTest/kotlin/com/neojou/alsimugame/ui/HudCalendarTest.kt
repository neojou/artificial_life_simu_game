package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class HudCalendarTest {

    @Test
    fun dayZero_isYear1Day1() {
        val c = calendarFromDay(0)
        assertEquals(1, c.year)
        assertEquals(1, c.dayInYear)
    }

    @Test
    fun endOfFirstYear() {
        val last = SimConfig.DAYS_PER_YEAR - 1
        val c = calendarFromDay(last)
        assertEquals(1, c.year)
        assertEquals(SimConfig.DAYS_PER_YEAR, c.dayInYear)
    }

    @Test
    fun startOfSecondYear() {
        val c = calendarFromDay(SimConfig.DAYS_PER_YEAR)
        assertEquals(2, c.year)
        assertEquals(1, c.dayInYear)
    }

    @Test
    fun midLifespan_mapsConsistently() {
        // day 25 → year 3, day 2  (25/12=2 → year 3, 25%12=1 → day 2)
        val c = calendarFromDay(25, daysPerYear = 12)
        assertEquals(3, c.year)
        assertEquals(2, c.dayInYear)
    }
}
