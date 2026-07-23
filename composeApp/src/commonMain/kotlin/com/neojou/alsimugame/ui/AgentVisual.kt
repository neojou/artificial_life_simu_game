package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender

/**
 * Display-layer pose for one agent (Vis-B).
 *
 * Logical simulation is still discrete (grid cells).
 * [progress] 0→1 interpolates from [fromX]/fromY] to [toX]/toY] in continuous
 * grid units (0 … GRID_SIZE).
 */
data class AgentVisual(
    val id: String,
    val gender: Gender,
    val mode: AgentMode,
    val carriedFood: Int,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val progress: Float,
    /** Horizontal slot when multiple agents share a cell (0, 1, …). */
    val slot: Int = 0,
) {
    val eased: Float
        get() = smoothstep(progress.coerceIn(0f, 1f))

    val displayX: Float
        get() = fromX + (toX - fromX) * eased + slot * 0.18f

    val displayY: Float
        get() = fromY + (toY - fromY) * eased

    val isMoving: Boolean
        get() = fromX != toX || fromY != toY

    companion object {
        fun atRest(agent: AgentSnapshot, slot: Int = 0): AgentVisual {
            val x = agent.x.toFloat()
            val y = agent.y.toFloat()
            return AgentVisual(
                id = agent.id,
                gender = agent.gender,
                mode = agent.mode,
                carriedFood = agent.carriedFood,
                fromX = x,
                fromY = y,
                toX = x,
                toY = y,
                progress = 1f,
                slot = slot,
            )
        }

        fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)
    }
}

/** Ease helper re-exported for tests. */
fun smoothstep(t: Float): Float = AgentVisual.smoothstep(t)
