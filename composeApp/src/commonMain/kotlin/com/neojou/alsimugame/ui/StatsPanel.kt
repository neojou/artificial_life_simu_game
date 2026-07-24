package com.neojou.alsimugame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.sim.model.TileState
import com.neojou.alsimugame.ui.theme.rememberAppFontFamily
import kotlin.math.roundToInt

/**
 * Stats panel (GDD §6.3): land counts, population, cumulative production.
 *
 * Vis-C: usually embedded in the Info dialog with [showToggle]=false and [expanded]=true.
 */
@Composable
fun StatsPanel(
    snapshot: SimSnapshot,
    expanded: Boolean,
    onToggle: () -> Unit = {},
    hoverText: String? = null,
    showToggle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val appFont = rememberAppFontFamily()
    val land = snapshot.landStateCounts()
    val grass = land[TileState.GRASS] ?: 0
    val farm = land[TileState.FARM] ?: 0
    val empty = land[TileState.EMPTY] ?: 0
    val avgAge = snapshot.averageLivingAgeDays()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "統計",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = appFont,
                fontWeight = FontWeight.SemiBold,
            )
            if (showToggle) {
                OutlinedButton(onClick = onToggle) {
                    Text(
                        text = if (expanded) "收合" else "展開",
                        fontFamily = appFont,
                    )
                }
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "土地：草 $grass · 田 $farm · 空 $empty",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = appFont,
                )
                Text(
                    "人口：存活 ${snapshot.livingAgentCount} · 死亡 ${snapshot.deadAgentCount} · " +
                        "平均存活日 ${avgAge.roundToInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = appFont,
                )
                Text(
                    "累計生產：田地產出 ${snapshot.totalFoodProduced} · " +
                        "採收 ${snapshot.totalFoodHarvested} · 入倉 ${snapshot.totalFoodDeposited}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = appFont,
                )
                Text(
                    "當前待收（田上）：${snapshot.totalPendingHarvest()} · 寨營庫存 ${snapshot.campFood}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = appFont,
                )
            }
        }

        if (!hoverText.isNullOrBlank()) {
            Text(
                text = "提示：$hoverText",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = appFont,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Human-readable agent mode for tooltips. */
fun agentModeLabel(mode: AgentMode): String = when (mode) {
    AgentMode.RESTING -> "休息中"
    AgentMode.SUPPLYING -> "補給中"
    AgentMode.EXPLORING -> "探索中"
    AgentMode.TILLING -> "正在開墾"
    AgentMode.HARVESTING -> "正在採收"
    AgentMode.RETURNING -> "返回寨營中"
    AgentMode.DEAD -> "已死亡"
}

/** Days remaining until FARM/EMPTY auto-transition; null if not applicable. */
fun daysUntilLandTransition(state: TileState, ageDays: Int): Int? = when (state) {
    TileState.FARM, TileState.EMPTY ->
        (SimConfig.LAND_STATE_DAYS - ageDays).coerceAtLeast(0)
    TileState.GRASS -> null
}

fun tileStateLabel(state: TileState): String = when (state) {
    TileState.GRASS -> "草地"
    TileState.FARM -> "田地"
    TileState.EMPTY -> "空地"
}
