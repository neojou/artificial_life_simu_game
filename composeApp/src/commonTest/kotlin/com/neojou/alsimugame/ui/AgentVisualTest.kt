package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.Gender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentVisualTest {

    @Test
    fun lerp_midpoint() {
        val v = AgentVisual(
            id = "a",
            gender = Gender.MALE,
            mode = AgentMode.EXPLORING,
            carriedFood = 0,
            fromX = 0f,
            fromY = 0f,
            toX = 2f,
            toY = 2f,
            progress = 0.5f,
            slot = 0,
        )
        // smoothstep(0.5) = 0.5
        assertEquals(1f, v.displayX, 1e-4f)
        assertEquals(1f, v.displayY, 1e-4f)
    }

    @Test
    fun smoothstep_endpoints() {
        assertEquals(0f, smoothstep(0f), 1e-6f)
        assertEquals(1f, smoothstep(1f), 1e-6f)
        assertTrue(smoothstep(0.5f) in 0.49f..0.51f)
    }

    @Test
    fun slot_shiftsDisplayX() {
        val base = AgentVisual(
            id = "a",
            gender = Gender.FEMALE,
            mode = AgentMode.RESTING,
            carriedFood = 0,
            fromX = 1f,
            fromY = 1f,
            toX = 1f,
            toY = 1f,
            progress = 1f,
            slot = 0,
        )
        val shifted = base.copy(slot = 1)
        assertTrue(shifted.displayX > base.displayX)
    }
}
