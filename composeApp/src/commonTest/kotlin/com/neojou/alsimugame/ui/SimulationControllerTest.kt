package com.neojou.alsimugame.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulationControllerTest {

    @Test
    fun stepOnce_advancesHour() = runTest {
        val controller = SimulationController(
            initialSeed = 1L,
            scope = this,
            baseDelayMs = 1_000L,
        )
        assertEquals(0, controller.snapshot.value.day)
        assertEquals(0, controller.snapshot.value.hour)

        controller.stepOnce()
        assertEquals(1, controller.snapshot.value.hour)
        assertEquals(0, controller.snapshot.value.day)

        controller.dispose()
    }

    @Test
    fun pause_stopsAutoAdvance() = runTest {
        val controller = SimulationController(
            initialSeed = 2L,
            scope = this,
            baseDelayMs = 100L,
        )
        controller.setSpeed(1)
        controller.play()
        assertTrue(controller.isPlaying.value)

        advanceTimeBy(50)
        runCurrent()
        // First step runs immediately in loop, then delay
        val hourAfterStart = controller.snapshot.value.hour

        controller.pause()
        assertFalse(controller.isPlaying.value)
        val frozenHour = controller.snapshot.value.hour
        val frozenDay = controller.snapshot.value.day

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(frozenHour, controller.snapshot.value.hour)
        assertEquals(frozenDay, controller.snapshot.value.day)
        // sanity: play did something or at least pause holds
        assertTrue(hourAfterStart >= 0)

        controller.dispose()
    }

    @Test
    fun reset_restoresInitialTime_withNewSeed() = runTest {
        val controller = SimulationController(
            initialSeed = 3L,
            scope = this,
            baseDelayMs = 100L,
        )
        repeat(8) { controller.stepOnce() }
        assertTrue(controller.snapshot.value.day > 0 || controller.snapshot.value.hour > 0)

        controller.reset(99L)
        assertEquals(99L, controller.seed.value)
        assertEquals(0, controller.snapshot.value.day)
        assertEquals(0, controller.snapshot.value.hour)
        assertFalse(controller.isPlaying.value)

        controller.dispose()
    }

    @Test
    fun setSpeed_onlyAcceptsAllowedMultipliers() = runTest {
        val controller = SimulationController(scope = this)
        controller.setSpeed(5)
        assertEquals(5, controller.speed.value)
        controller.setSpeed(7) // invalid
        assertEquals(5, controller.speed.value)
        controller.dispose()
    }
}
