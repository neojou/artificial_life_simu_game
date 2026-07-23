package com.neojou.alsimugame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
 * Simulation screen: top [HudView], board, controls.
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

    // Fixed light background for comfort; day/night is shown only in the HUD (not full-screen tint).
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
            // M4-T1: persistent top HUD (year/day/phase + camp food)
            HudView(snapshot = snapshot)

            // Secondary debug line (seed / play / agents) — not the main HUD
            SecondaryStatusLine(
                snapshot = snapshot,
                seed = seed,
                speed = speed,
                playing = playing,
            )

            HorizontalDivider()

            BoardView(
                snapshot = snapshot,
                frameId = frame.id,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            HorizontalDivider()

            ControlRow(
                playing = playing,
                speed = speed,
                speedOptions = controller.speedOptions,
                gameOver = snapshot.isGameOver,
                onPlay = controller::play,
                onPause = controller::pause,
                onStep = controller::stepOnce,
                onSpeed = controller::setSpeed,
                onReset = { controller.reset(seed) },
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
    val land = snapshot.landStateCounts()
    val grass = land[com.neojou.alsimugame.sim.model.TileState.GRASS] ?: 0
    val farm = land[com.neojou.alsimugame.sim.model.TileState.FARM] ?: 0
    val empty = land[com.neojou.alsimugame.sim.model.TileState.EMPTY] ?: 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "$status · Seed $seed · ${speed}× · 村民 ${snapshot.livingAgentCount}/${snapshot.agents.size} · " +
                "草$grass / 田$farm / 空$empty",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        snapshot.agents.forEach { agent ->
            Text(
                text = "· ${agent.id} ${agent.gender} @(${agent.x},${agent.y}) " +
                    "sta=${agent.stamina} ${agent.mode} carry=${agent.carriedFood}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ControlRow(
    playing: Boolean,
    speed: Int,
    speedOptions: List<Int>,
    gameOver: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStep: () -> Unit,
    onSpeed: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (playing) {
                Button(onClick = onPause) { Text("暫停") }
            } else {
                Button(onClick = onPlay, enabled = !gameOver) { Text("播放") }
            }
            OutlinedButton(onClick = onStep, enabled = !gameOver) { Text("單步") }
            OutlinedButton(onClick = onReset) { Text("重置") }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("速度", style = MaterialTheme.typography.bodyMedium)
            speedOptions.forEach { option ->
                if (option == speed) {
                    Button(onClick = { onSpeed(option) }) { Text("${option}×") }
                } else {
                    OutlinedButton(onClick = { onSpeed(option) }) { Text("${option}×") }
                }
            }
        }
    }
}
