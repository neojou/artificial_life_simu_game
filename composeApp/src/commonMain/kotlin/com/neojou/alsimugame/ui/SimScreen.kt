package com.neojou.alsimugame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neojou.alsimugame.sim.model.SimSnapshot

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
 * Simulation screen: HUD, stats, board (with hover), controls.
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

    var statsExpanded by remember { mutableStateOf(true) }
    var hoverInfo by remember { mutableStateOf<BoardHoverInfo?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudView(snapshot = snapshot)

            SecondaryStatusLine(
                snapshot = snapshot,
                seed = seed,
                speed = speed,
                playing = playing,
            )

            StatsPanel(
                snapshot = snapshot,
                expanded = statsExpanded,
                onToggle = { statsExpanded = !statsExpanded },
                hoverText = hoverInfo?.summary,
            )

            HorizontalDivider()

            BoardView(
                snapshot = snapshot,
                frameId = frame.id,
                agentVisuals = frame.agentVisuals,
                onHover = { hoverInfo = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            HorizontalDivider()

            ControlsView(
                playing = playing,
                speed = speed,
                speedOptions = controller.speedOptions,
                currentSeed = seed,
                gameOver = snapshot.isGameOver,
                onPlay = controller::play,
                onPause = controller::pause,
                onStep = controller::stepOnce,
                onSpeed = controller::setSpeed,
                onReset = { newSeed ->
                    hoverInfo = null
                    controller.reset(newSeed)
                },
            )
        }
    }
}

@Composable
private fun SecondaryStatusLine(
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}
