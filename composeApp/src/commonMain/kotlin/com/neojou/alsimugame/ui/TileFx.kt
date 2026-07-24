package com.neojou.alsimugame.ui

import androidx.compose.ui.graphics.Color
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.TileSnapshot
import com.neojou.alsimugame.sim.model.TileState

/**
 * Brief work feedback kind shown while an agent spends an hour tilling/harvesting (M5-T3).
 */
enum class WorkFxKind {
    TILL,
    HARVEST,
}

/**
 * True when a farm tile has pending food and should pulse-highlight (M5-T3).
 * Pure helper — no Compose — for unit tests and UI.
 */
fun isHarvestHighlight(tile: TileSnapshot?): Boolean =
    tile != null &&
        tile.state == TileState.FARM &&
        tile.pendingHarvest > 0

/**
 * Maps agent mode → optional cell/pawn work FX. Null = no feedback burst.
 */
fun workFxKind(mode: AgentMode): WorkFxKind? = when (mode) {
    AgentMode.TILLING -> WorkFxKind.TILL
    AgentMode.HARVESTING -> WorkFxKind.HARVEST
    else -> null
}

/** Soft gold wash over ripe farms. */
val HarvestHighlightFill: Color = Color(0xFFFFF176).copy(alpha = 0.22f)

/** Border for ripe farms (alpha modulated by animation). */
val HarvestHighlightBorder: Color = Color(0xFFFFB300)

val TillFxColor: Color = Color(0xFF8D6E63)
val HarvestFxColor: Color = Color(0xFFFFD54F)

fun workFxLabel(kind: WorkFxKind): String = when (kind) {
    WorkFxKind.TILL -> "墾"
    WorkFxKind.HARVEST -> "收"
}

fun workFxColor(kind: WorkFxKind): Color = when (kind) {
    WorkFxKind.TILL -> TillFxColor
    WorkFxKind.HARVEST -> HarvestFxColor
}
