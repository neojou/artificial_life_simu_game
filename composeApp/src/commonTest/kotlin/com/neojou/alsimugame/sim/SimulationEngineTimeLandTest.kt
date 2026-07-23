package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulationEngineTimeLandTest {

    @Test
    fun stepHour_advancesClock_withoutDayWrapUntilSixthHour() {
        val engine = SimulationEngine()
        assertEquals(0, engine.clock.day)
        assertEquals(0, engine.clock.hour)
        assertTrue(engine.clock.isDay)

        engine.stepHour()
        assertEquals(1, engine.clock.hour)
        assertEquals(0, engine.clock.day)

        repeat(4) { engine.stepHour() }
        assertEquals(5, engine.clock.hour)
        assertTrue(engine.clock.isNight)
        assertEquals(0, engine.clock.day)

        engine.stepHour()
        assertEquals(0, engine.clock.hour)
        assertEquals(1, engine.clock.day)
        assertTrue(engine.clock.isDay)
    }

    @Test
    fun runDays_advancesDayCounter() {
        val engine = SimulationEngine()
        engine.runDays(2)
        assertEquals(2, engine.clock.day)
        assertEquals(0, engine.clock.hour)
        assertEquals(2 * SimConfig.HOURS_PER_DAY, 12)
    }

    @Test
    fun midDay_isNightFlagsTrackHour() {
        val engine = SimulationEngine()
        // hour 0,1,2 day; after 3 steps → hour 3 night
        repeat(3) { engine.stepHour() }
        assertEquals(3, engine.clock.hour)
        assertTrue(engine.clock.isNight)
        assertFalse(engine.clock.isDay)
    }
}
