package com.neojou.alsimugame.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.neojou.alsimugame.composeapp.generated.resources.Res
import com.neojou.alsimugame.composeapp.generated.resources.tile_camp
import com.neojou.alsimugame.composeapp.generated.resources.tile_empty
import com.neojou.alsimugame.composeapp.generated.resources.tile_farm
import com.neojou.alsimugame.composeapp.generated.resources.tile_grass
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.sim.model.TileSnapshot
import com.neojou.alsimugame.sim.model.TileState
import com.neojou.alsimugame.ui.theme.rememberAppFontFamily
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Soft paper-like board surround. */
private val BoardMatte = Color(0xFFF3EDE3)
private val CellGapColor = Color(0xFFE8E0D4)
private val HoverBorder = Color(0xFF5D8AA8)

/** Hover target for board tooltips (GDD §6.3). */
data class BoardHoverInfo(
    val x: Int,
    val y: Int,
    val summary: String,
)

/**
 * Top-down RimWorld-style board: painted tiles + chibi pawn sprites.
 */
@Composable
fun BoardView(
    snapshot: SimSnapshot,
    modifier: Modifier = Modifier,
    gridSize: Int = SimConfig.GRID_SIZE,
    frameId: Long = 0L,
    onHover: (BoardHoverInfo?) -> Unit = {},
) {
    val appFont = rememberAppFontFamily()
    val tileByPos = remember(frameId, snapshot.tiles) {
        snapshot.tiles.associateBy { it.x to it.y }
    }
    val agentsByPos = remember(frameId, snapshot.agents) {
        snapshot.agents.groupBy { it.x to it.y }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoardMatte, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "小小寨營",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = appFont,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5D4E37),
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CellGapColor, RoundedCornerShape(8.dp))
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            val gap = 2.dp
            val side = min(maxWidth, maxHeight) * 0.99f
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
                                campFood = snapshot.campFood,
                                onHover = onHover,
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
    campFood: Int,
    onHover: (BoardHoverInfo?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appFont = rememberAppFontFamily()
    val tileRes = WorldAssets.tileFor(isCamp, tile)
    val hoverSummary = remember(x, y, isCamp, tile, agents, campFood) {
        buildHoverSummary(x, y, isCamp, tile, agents, campFood)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(hoverSummary) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> {
                                onHover(BoardHoverInfo(x, y, hoverSummary))
                            }
                            PointerEventType.Exit -> onHover(null)
                            else -> Unit
                        }
                    }
                }
            },
    ) {
        Image(
            painter = painterResource(tileRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Optional tiny label for farm pending
        if (tile != null && tile.state == TileState.FARM && tile.pendingHarvest > 0) {
            Text(
                text = "×${tile.pendingHarvest}",
                color = Color(0xFF5D4037),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFont,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            )
        }

        // Pawns stacked near cell center
        if (agents.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy((-4).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                agents.forEach { agent ->
                    val alpha = if (agent.mode == AgentMode.DEAD) 0.4f else 1f
                    Image(
                        painter = painterResource(WorldAssets.pawnFor(agent)),
                        contentDescription = agent.id,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight(0.75f)
                            .fillMaxWidth(if (agents.size > 1) 0.42f else 0.55f),
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

private fun buildHoverSummary(
    x: Int,
    y: Int,
    isCamp: Boolean,
    tile: TileSnapshot?,
    agents: List<AgentSnapshot>,
    campFood: Int,
): String {
    val parts = mutableListOf<String>()
    parts += "格 ($x,$y)"
    if (isCamp) {
        parts += "寨營 · 庫存 $campFood · 不可耕種"
    } else if (tile != null) {
        val remain = daysUntilLandTransition(tile.state, tile.ageDays)
        val remainText = remain?.let { "距離轉換 ${it} 日" } ?: "開墾後進入田地循環"
        parts += "${tileStateLabel(tile.state)} · 年齡 ${tile.ageDays} 日 · $remainText"
        if (tile.state == TileState.FARM) {
            parts += "待收 ${tile.pendingHarvest}/${SimConfig.MAX_PENDING_HARVEST}"
        }
    }
    if (agents.isNotEmpty()) {
        agents.forEach { a ->
            parts += "${a.id}(${if (a.gender == Gender.MALE) "男" else "女"}) " +
                "體力 ${a.stamina}/${SimConfig.MAX_STAMINA} · 攜帶 ${a.carriedFood} · " +
                agentModeLabel(a.mode) +
                if (a.returnHome) " · 回營中" else ""
        }
    }
    return parts.joinToString(" ｜ ")
}

@Composable
private fun BoardLegend() {
    val appFont = rememberAppFontFamily()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendTile(Res.drawable.tile_grass, "草地", appFont)
        LegendTile(Res.drawable.tile_farm, "田地", appFont)
        LegendTile(Res.drawable.tile_empty, "空地", appFont)
        LegendTile(Res.drawable.tile_camp, "寨營", appFont)
    }
}

@Composable
private fun LegendTile(
    res: DrawableResource,
    label: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = fontFamily,
        )
    }
}
