package com.neojou.alsimugame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.ui.theme.rememberAppFontFamily
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.sim.model.TileSnapshot
import com.neojou.alsimugame.sim.model.TileState

/** Placeholder palette for land / camp (M3-T2; no art assets). */
object BoardColors {
    val Camp = Color(0xFFB08968)
    val Grass = Color(0xFF7CB342)
    val Farm = Color(0xFFA1887F)
    val Empty = Color(0xFF9E9E9E)
    val CellBorder = Color(0xFF5D4037)
    val MaleMarker = Color(0xFF1976D2)
    val FemaleMarker = Color(0xFFC2185B)
    val DeadMarker = Color(0xFF616161)
    val LabelOnDark = Color(0xFFFFFFFF)
    val LabelOnLight = Color(0xFF212121)
}

/**
 * Fixed top-down board ([SimConfig.GRID_SIZE]²) driven only by [SimSnapshot].
 *
 * Sizing: the grid is a square that fits inside the **smaller** of the parent's
 * max width and max height (via [BoxWithConstraints]), so all 9 cells stay visible
 * when the window is short or wide. Does not force height = full width.
 */
@Composable
fun BoardView(
    snapshot: SimSnapshot,
    modifier: Modifier = Modifier,
    gridSize: Int = SimConfig.GRID_SIZE,
) {
    val appFont = rememberAppFontFamily()
    val tileByPos = remember(snapshot.tiles) {
        snapshot.tiles.associateBy { it.x to it.y }
    }
    val agentsByPos = remember(snapshot.agents) {
        snapshot.agents.groupBy { it.x to it.y }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "地圖 ${gridSize}×${gridSize}",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = appFont,
        )

        // Grid area: take remaining height, center a square that fits both axes.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val gap = 4.dp
            // Leave a little inset so borders aren't clipped.
            val side = min(maxWidth, maxHeight) * 0.98f
            Column(
                modifier = Modifier.size(side),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                for (y in 0 until gridSize) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        for (x in 0 until gridSize) {
                            val isCamp = x == SimConfig.CAMP_X && y == SimConfig.CAMP_Y
                            val tile = tileByPos[x to y]
                            val agents = agentsByPos[x to y].orEmpty()
                            BoardCell(
                                x = x,
                                y = y,
                                isCamp = isCamp,
                                tile = tile,
                                agents = agents,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        BoardLegend()
    }
}

@Composable
private fun BoardCell(
    x: Int,
    y: Int,
    isCamp: Boolean,
    tile: TileSnapshot?,
    agents: List<AgentSnapshot>,
    modifier: Modifier = Modifier,
) {
    val appFont = rememberAppFontFamily()
    val bg = when {
        isCamp -> BoardColors.Camp
        tile == null -> BoardColors.Empty
        else -> colorForTileState(tile.state)
    }
    val labelColor =
        if (isCamp || tile?.state == TileState.FARM || tile?.state == TileState.EMPTY) {
            BoardColors.LabelOnDark
        } else {
            BoardColors.LabelOnLight
        }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.5.dp, BoardColors.CellBorder, RoundedCornerShape(6.dp))
            .padding(3.dp),
    ) {
        Text(
            text = when {
                isCamp -> "寨營"
                tile == null -> "?"
                else -> shortState(tile.state)
            },
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = appFont,
            modifier = Modifier.align(Alignment.TopStart),
        )

        if (tile != null && tile.state == TileState.FARM && tile.pendingHarvest > 0) {
            Text(
                text = "穗${tile.pendingHarvest}",
                color = labelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFont,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        if (agents.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                agents.forEach { agent ->
                    AgentMarker(agent = agent, fontFamily = appFont)
                }
            }
        }

        Text(
            text = "$x,$y",
            color = labelColor.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = appFont,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun AgentMarker(
    agent: AgentSnapshot,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
) {
    val alive = agent.mode != AgentMode.DEAD
    val color = when {
        !alive -> BoardColors.DeadMarker
        agent.gender == Gender.MALE -> BoardColors.MaleMarker
        else -> BoardColors.FemaleMarker
    }
    val letter = when {
        !alive -> "×"
        agent.gender == Gender.MALE -> "M"
        else -> "F"
    }
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(color, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoardLegend() {
    val appFont = rememberAppFontFamily()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        LegendSwatch(BoardColors.Camp, "寨營", appFont)
        LegendSwatch(BoardColors.Grass, "草地", appFont)
        LegendSwatch(BoardColors.Farm, "田地", appFont)
        LegendSwatch(BoardColors.Empty, "空地", appFont)
        Text(
            "M/F=村民",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = appFont,
        )
    }
}

@Composable
private fun LegendSwatch(
    color: Color,
    label: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
                .border(0.5.dp, BoardColors.CellBorder, RoundedCornerShape(2.dp)),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = fontFamily,
        )
    }
}

private fun colorForTileState(state: TileState): Color = when (state) {
    TileState.GRASS -> BoardColors.Grass
    TileState.FARM -> BoardColors.Farm
    TileState.EMPTY -> BoardColors.Empty
}

private fun shortState(state: TileState): String = when (state) {
    TileState.GRASS -> "草"
    TileState.FARM -> "田"
    TileState.EMPTY -> "空"
}
