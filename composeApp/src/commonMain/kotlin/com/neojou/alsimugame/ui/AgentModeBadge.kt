package com.neojou.alsimugame.ui

import androidx.compose.ui.graphics.Color
import com.neojou.alsimugame.sim.model.AgentMode

/**
 * Compact on-board label for an agent mode (M5-T2).
 *
 * Uses single CJK glyphs (NotoSansTC-safe) + distinct colors so at least
 * REST / EXPLORE / WORK / RETURN / DEAD read clearly without walk sprites.
 */
data class AgentModeBadge(
    val mode: AgentMode,
    /** One-character label drawn on the badge. */
    val glyph: String,
    val background: Color,
    val content: Color = Color.White,
    /** Soft vertical bob while this mode is active. */
    val bob: Boolean = false,
    /** Gentle scale pulse (work actions). */
    val pulse: Boolean = false,
)

/**
 * Maps [AgentMode] → visual badge. Pure function for unit tests.
 */
fun agentModeBadge(mode: AgentMode): AgentModeBadge = when (mode) {
    AgentMode.RESTING -> AgentModeBadge(
        mode = mode,
        glyph = "休",
        background = Color(0xFF5B8FB9),
        bob = true,
    )
    AgentMode.SUPPLYING -> AgentModeBadge(
        mode = mode,
        glyph = "補",
        background = Color(0xFFC9A227),
    )
    AgentMode.EXPLORING -> AgentModeBadge(
        mode = mode,
        glyph = "探",
        background = Color(0xFF4CAF50),
    )
    AgentMode.TILLING -> AgentModeBadge(
        mode = mode,
        glyph = "墾",
        background = Color(0xFF8D6E63),
        pulse = true,
    )
    AgentMode.HARVESTING -> AgentModeBadge(
        mode = mode,
        glyph = "收",
        background = Color(0xFFE67E22),
        pulse = true,
    )
    AgentMode.RETURNING -> AgentModeBadge(
        mode = mode,
        glyph = "回",
        background = Color(0xFF7E57C2),
    )
    AgentMode.DEAD -> AgentModeBadge(
        mode = mode,
        glyph = "亡",
        background = Color(0xFF616161),
        content = Color(0xFFEEEEEE),
    )
}

/**
 * At least these modes must have pairwise-distinct glyphs (DoD: 4+ modes).
 */
val PRIMARY_MODE_GLYPHS: Map<AgentMode, String> = mapOf(
    AgentMode.RESTING to "休",
    AgentMode.EXPLORING to "探",
    AgentMode.TILLING to "墾",
    AgentMode.HARVESTING to "收",
    AgentMode.RETURNING to "回",
    AgentMode.DEAD to "亡",
    AgentMode.SUPPLYING to "補",
)
