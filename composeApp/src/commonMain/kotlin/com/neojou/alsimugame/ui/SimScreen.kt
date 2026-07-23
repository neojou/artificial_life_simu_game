package com.neojou.alsimugame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neojou.alsimugame.sim.model.SimSnapshot

/**
 * Creates and remembers a [SimulationController] bound to the composition scope.
 */
@Composable
fun rememberSimulationController(
    initialSeed: Long = SimulationController.DEFAULT_SEED,
): SimulationController {
    val scope = rememberCoroutineScope()
    val controller = remember(initialSeed) {
        SimulationController(initialSeed = initialSeed, scope = scope)
    }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}

/**
 * Minimal simulation screen for M3-T1: text readout + play/pause/speed/reset.
 * Full board UI arrives in M3-T2.
 */
@Composable
fun SimScreen(
    controller: SimulationController = rememberSimulationController(),
) {
    val snapshot by controller.snapshot.collectAsState()
    val playing by controller.isPlaying.collectAsState()
    val speed by controller.speed.collectAsState()
    val seed by controller.seed.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "小小寨營 / Tiny Camp",
                style = MaterialTheme.typography.headlineSmall,
            )
            SnapshotReadout(snapshot = snapshot, seed = seed, speed = speed, playing = playing)
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
            Text(
                text = "M3-T1: text readout only — board UI in M3-T2",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SnapshotReadout(
    snapshot: SimSnapshot,
    seed: Long,
    speed: Int,
    playing: Boolean,
) {
    val phase = if (snapshot.isDay) "白天" else "夜晚"
    val status = when {
        snapshot.isGameOver -> "結束 (Game Over)"
        playing -> "播放中"
        else -> "暫停"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("狀態: $status")
        Text("Seed: $seed")
        Text("時間: 第 ${snapshot.day} 日 / 時辰 ${snapshot.hour} ($phase)")
        Text("寨營糧食: ${snapshot.campFood}")
        Text("村民: ${snapshot.livingAgentCount} 存活 / ${snapshot.agents.size} 總計")
        Text("速度: ${speed}×")
        snapshot.agents.forEach { agent ->
            Text(
                "  · ${agent.id} [${agent.gender}] " +
                    "pos=(${agent.x},${agent.y}) " +
                    "sta=${agent.stamina} mode=${agent.mode} " +
                    "carry=${agent.carriedFood}",
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text("速度")
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
