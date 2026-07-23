package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.model.TileState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulationControllerTest {

    @Test
    fun stepOnce_advancesHour_andFrameId() = runTest {
        val controller = SimulationController(
            initialSeed = 1L,
            scope = this,
            baseDelayMs = 1_000L,
            initialSpeed = 1,
        )
        assertEquals(0, controller.frame.value.snapshot.day)
        assertEquals(0, controller.frame.value.snapshot.hour)
        val id0 = controller.frame.value.id

        controller.stepOnce()
        assertEquals(1, controller.frame.value.snapshot.hour)
        assertEquals(0, controller.frame.value.snapshot.day)
        assertTrue(controller.frame.value.id > id0)

        controller.dispose()
    }

    @Test
    fun pause_stopsAutoAdvance() = runTest {
        val controller = SimulationController(
            initialSeed = 2L,
            scope = this,
            baseDelayMs = 100L,
            initialSpeed = 1,
        )
        controller.setSpeed(1)
        controller.play()
        assertTrue(controller.isPlaying.value)

        advanceTimeBy(50)
        runCurrent()

        controller.pause()
        assertFalse(controller.isPlaying.value)
        val frozen = controller.frame.value

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(frozen.id, controller.frame.value.id)
        assertEquals(frozen.snapshot.hour, controller.frame.value.snapshot.hour)
        assertEquals(frozen.snapshot.day, controller.frame.value.snapshot.day)

        controller.dispose()
    }

    @Test
    fun reset_restoresInitialTime_withNewSeed() = runTest {
        val controller = SimulationController(
            initialSeed = 3L,
            scope = this,
            baseDelayMs = 100L,
            initialSpeed = 1,
        )
        repeat(8) { controller.stepOnce() }
        assertTrue(
            controller.frame.value.snapshot.day > 0 ||
                controller.frame.value.snapshot.hour > 0,
        )

        controller.reset(99L)
        assertEquals(99L, controller.seed.value)
        assertEquals(0, controller.frame.value.snapshot.day)
        assertEquals(0, controller.frame.value.snapshot.hour)
        assertFalse(controller.isPlaying.value)

        controller.dispose()
    }

    @Test
    fun setSpeed_onlyAcceptsAllowedMultipliers() = runTest {
        val controller = SimulationController(scope = this, initialSpeed = 1)
        controller.setSpeed(5)
        assertEquals(5, controller.speed.value)
        controller.setSpeed(7)
        assertEquals(5, controller.speed.value)
        controller.dispose()
    }

    @Test
    fun playLoop_advancesManyHours_andWorldCanChange() = runTest {
        val controller = SimulationController(
            initialSeed = 0L,
            scope = this,
            baseDelayMs = 20L,
            initialSpeed = 10,
        )
        val start = controller.frame.value.snapshot
        controller.play()

        // ~30s wall at 5×/280ms is many hours; here we advance virtual time generously.
        advanceTimeBy(10_000)
        runCurrent()

        val after = controller.frame.value.snapshot
        assertTrue(after.day > start.day || after.hour != start.hour || controller.frame.value.id > 0)
        assertTrue(controller.frame.value.id >= 1)

        // Over enough steps, AI usually leaves camp or tills (not guaranteed every seed,
        // but seed 0 + many hours is very likely to show activity).
        val posChanged = after.agents.zip(start.agents).any { (a, b) -> a.x != b.x || a.y != b.y }
        val landChanged = after.tiles != start.tiles
        val timeMoved = after.day != start.day || after.hour != start.hour
        assertTrue(timeMoved, "time must advance while playing")
        // Living sim: either agents moved or land state/pending changed for typical seeds
        assertTrue(
            posChanged || landChanged || after.totalPendingHarvest() > 0 ||
                (after.landStateCounts()[TileState.FARM] ?: 0) > 0,
            "expected visible world activity (agents/land) after play; " +
                "posChanged=$posChanged landChanged=$landChanged",
        )

        controller.pause()
        controller.dispose()
    }

    @Test
    fun snapshotMirror_matchesFrame() = runTest {
        val controller = SimulationController(scope = this, initialSpeed = 1)
        controller.stepOnce()
        assertEquals(controller.frame.value.snapshot, controller.snapshot.value)
        assertNotEquals(0L, controller.frame.value.id)
        controller.dispose()
    }

    @Test
    fun reset_sameSeed_reproducesAfterSteps() = runTest {
        val seed = 42L
        val a = SimulationController(initialSeed = seed, scope = this, initialSpeed = 1)
        repeat(12) { a.stepOnce() }
        val mid = a.frame.value.snapshot

        a.reset(seed)
        assertEquals(0, a.frame.value.snapshot.day)
        assertEquals(0, a.frame.value.snapshot.hour)
        assertEquals(seed, a.seed.value)

        repeat(12) { a.stepOnce() }
        assertEquals(mid, a.frame.value.snapshot)

        a.dispose()
    }

    @Test
    fun higherSpeed_hasShorterDelay() = runTest {
        val c = SimulationController(scope = this, baseDelayMs = 1000L, initialSpeed = 1)
        c.setSpeed(1)
        val d1 = c.delayMsForCurrentSpeed()
        c.setSpeed(10)
        val d10 = c.delayMsForCurrentSpeed()
        assertTrue(d10 < d1)
        assertEquals(100L, d10) // 1000/10
        c.dispose()
    }
}
