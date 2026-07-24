package com.neojou.alsimugame.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.sim.model.TileSnapshot
import com.neojou.alsimugame.sim.model.TileState
import com.neojou.alsimugame.ui.theme.rememberAppFontFamily
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/** Soft paper-like board surround (full-bleed under tiles). */
private val BoardMatte = Color(0xFFF3EDE3)

/** Hover target for board tooltips (GDD §6.3). */
data class BoardHoverInfo(
    val x: Int,
    val y: Int,
    val summary: String,
)

/**
 * Top-down RimWorld-style board: seamless tiles + smoothly interpolated pawns.
 *
 * Vis-C: full-bleed map only (no title / legend chrome).
 */
@Composable
fun BoardView(
    snapshot: SimSnapshot,
    modifier: Modifier = Modifier,
    gridSize: Int = SimConfig.GRID_SIZE,
    frameId: Long = 0L,
    agentVisuals: List<AgentVisual> = emptyList(),
    onHover: (BoardHoverInfo?) -> Unit = {},
) {
    val tileByPos = remember(frameId, snapshot.tiles) {
        snapshot.tiles.associateBy { it.x to it.y }
    }
    val agentsByPos = remember(frameId, snapshot.agents) {
        snapshot.agents.groupBy { it.x to it.y }
    }
    val visuals = if (agentVisuals.isNotEmpty()) {
        agentVisuals
    } else {
        val slots = mutableMapOf<Pair<Int, Int>, Int>()
        snapshot.agents.map { a ->
            val key = a.x to a.y
            val slot = slots[key] ?: 0
            slots[key] = slot + 1
            AgentVisual.atRest(a, slot)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BoardMatte),
        contentAlignment = Alignment.Center,
    ) {
        val side = min(maxWidth, maxHeight)
        val cell: Dp = side / gridSize
        val density = LocalDensity.current

        Box(modifier = Modifier.size(side)) {
            // Seamless tile grid (gap = 0)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                for (y in 0 until gridSize) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        for (x in 0 until gridSize) {
                            val isCamp = x == SimConfig.CAMP_X && y == SimConfig.CAMP_Y
                            val tile = tileByPos[x to y]
                            val agentsHere = agentsByPos[x to y].orEmpty()
                            TileOnlyCell(
                                x = x,
                                y = y,
                                isCamp = isCamp,
                                tile = tile,
                                agents = agentsHere,
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

            // Pawns + mode badges (Vis-B move + M5-T2 mode cues)
            visuals.forEach { visual ->
                val pawnSize = cell * 0.72f
                val offsetX = with(density) {
                    (cell * visual.displayX + (cell - pawnSize) / 2f).toPx()
                }
                val offsetY = with(density) {
                    (cell * visual.displayY + (cell - pawnSize) / 2f).toPx()
                }
                AgentPawn(
                    visual = visual,
                    pawnSize = pawnSize,
                    modifier = Modifier.offset {
                        IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                    },
                )
            }
        }
    }
}

/**
 * Pawn sprite + mode badge with light bob/pulse (M5-T2).
 */
@Composable
private fun AgentPawn(
    visual: AgentVisual,
    pawnSize: Dp,
    modifier: Modifier = Modifier,
) {
    val appFont = rememberAppFontFamily()
    val badge = agentModeBadge(visual.mode)
    val baseAlpha = if (visual.mode == AgentMode.DEAD) 0.42f else 1f

    val transition = rememberInfiniteTransition(label = "pawn-${visual.id}")
    val bobPx by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (badge.bob) 3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob-${visual.id}",
    )
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (badge.pulse) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-${visual.id}",
    )

    val pseudo = AgentSnapshot(
        id = visual.id,
        gender = visual.gender,
        x = visual.toX.roundToInt(),
        y = visual.toY.roundToInt(),
        stamina = 0,
        carriedFood = visual.carriedFood,
        ageDays = 0,
        mode = visual.mode,
        returnHome = false,
    )

    val badgeSize = pawnSize * 0.38f

    Box(
        modifier = modifier
            .size(pawnSize)
            .offset { IntOffset(0, (-bobPx).roundToInt()) }
            .scale(pulseScale)
            .alpha(baseAlpha),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(WorldAssets.pawnFor(pseudo)),
            contentDescription = "${visual.id} ${agentModeLabel(visual.mode)}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        // Mode badge above head
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-badgeSize * 0.15f))
                .size(badgeSize)
                .background(badge.background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge.glyph,
                color = badge.content,
                fontSize = (badgeSize.value * 0.55f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFont,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TileOnlyCell(
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
