package com.neojou.alsimugame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.ui.theme.rememberAppFontFamily
import com.neojou.tools.ui.menu.MyTopMenuBar
import com.neojou.tools.ui.menu.MyTopMenuItem

/**
 * Creates and remembers a [SimulationController] bound to the composition scope.
 */
@Composable
fun rememberSimulationController(
    initialSeed: Long = SimulationController.DEFAULT_SEED,
    autoPlay: Boolean = true,
): SimulationController {
    val scope = rememberCoroutineScope()
    val controller = remember(initialSeed) {
        SimulationController(initialSeed = initialSeed, scope = scope)
    }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    LaunchedEffect(controller, autoPlay) {
        if (autoPlay) {
            controller.play()
        }
    }
    return controller
}

/**
 * Immersive simulation screen (Vis-C):
 * top menu + full-bleed board; Info / Settings as dialogs.
 */
@Composable
fun SimScreen(
    controller: SimulationController = rememberSimulationController(),
) {
    val frame by controller.frame.collectAsState()
    val snapshot = frame.snapshot
    val playing by controller.isPlaying.collectAsState()
    val speed by controller.speed.collectAsState()
    val seed by controller.seed.collectAsState()

    var showInfo by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var hoverInfo by remember { mutableStateOf<BoardHoverInfo?>(null) }

    val menuItems = listOf(
        MyTopMenuItem(
            id = "info",
            label = "Info",
            onClick = { showInfo = true },
        ),
        MyTopMenuItem(
            id = "settings",
            label = "Settings",
            onClick = { showSettings = true },
        ),
        MyTopMenuItem(
            id = "play",
            label = if (playing) "暫停" else "播放",
            enabled = playing || !snapshot.isGameOver,
            onClick = {
                if (playing) controller.pause() else controller.play()
            },
        ),
        MyTopMenuItem(
            id = "step",
            label = "單步",
            enabled = !snapshot.isGameOver,
            onClick = { controller.stepOnce() },
        ),
        MyTopMenuItem(
            id = "reset",
            label = "重置",
            onClick = {
                hoverInfo = null
                controller.reset(seed)
            },
        ),
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MyTopMenuBar(items = menuItems)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                BoardView(
                    snapshot = snapshot,
                    frameId = frame.id,
                    agentVisuals = frame.agentVisuals,
                    onHover = { hoverInfo = it },
                    modifier = Modifier.fillMaxSize(),
                )

                BoardHoverTooltip(
                    hoverInfo = hoverInfo,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth(0.96f),
                )
            }
        }
    }

    if (showInfo) {
        InfoDialog(
            snapshot = snapshot,
            seed = seed,
            speed = speed,
            playing = playing,
            hoverText = hoverInfo?.summary,
            onDismiss = { showInfo = false },
        )
    }

    if (showSettings) {
        SettingsDialog(
            speed = speed,
            speedOptions = controller.speedOptions,
            currentSeed = seed,
            onSpeed = controller::setSpeed,
            onApplySeed = { newSeed, resumePlay ->
                hoverInfo = null
                controller.reset(newSeed)
                if (resumePlay) controller.play()
            },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun BoardHoverTooltip(
    hoverInfo: BoardHoverInfo?,
    modifier: Modifier = Modifier,
) {
    if (hoverInfo == null) return
    val appFont = rememberAppFontFamily()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
    ) {
        Text(
            text = hoverInfo.summary,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = appFont,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun InfoDialog(
    snapshot: SimSnapshot,
    seed: Long,
    speed: Int,
    playing: Boolean,
    hoverText: String?,
    onDismiss: () -> Unit,
) {
    val appFont = rememberAppFontFamily()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Info",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = appFont,
                    fontWeight = FontWeight.SemiBold,
                )

                HudView(snapshot = snapshot)

                StatusLine(
                    snapshot = snapshot,
                    seed = seed,
                    speed = speed,
                    playing = playing,
                )

                HorizontalDivider()

                StatsPanel(
                    snapshot = snapshot,
                    expanded = true,
                    showToggle = false,
                    hoverText = null,
                )

                HorizontalDivider()

                Text(
                    text = "目前指向",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = appFont,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = hoverText?.takeIf { it.isNotBlank() } ?: "（尚未指向土地）",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = appFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("關閉", fontFamily = appFont)
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    speed: Int,
    speedOptions: List<Int>,
    currentSeed: Long,
    onSpeed: (Int) -> Unit,
    onApplySeed: (seed: Long, resumePlay: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val appFont = rememberAppFontFamily()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsPanel(
                    speed = speed,
                    speedOptions = speedOptions,
                    currentSeed = currentSeed,
                    onSpeed = onSpeed,
                    onApplySeed = { seed, resume ->
                        onApplySeed(seed, resume)
                        onDismiss()
                    },
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("關閉", fontFamily = appFont)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(
    snapshot: SimSnapshot,
    seed: Long,
    speed: Int,
    playing: Boolean,
) {
    val status = when {
        snapshot.isGameOver -> "結束"
        playing -> "播放中"
        else -> "暫停"
    }

    Text(
        text = "$status · Seed $seed · ${speed}× · 村民 ${snapshot.livingAgentCount}/${snapshot.agents.size}",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}
