package com.neojou.alsimugame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neojou.alsimugame.ui.theme.rememberAppFontFamily

/**
 * Bottom control strip (GDD §6.3): play/pause, step, speed, reset, seed input.
 */
@Composable
fun ControlsView(
    playing: Boolean,
    speed: Int,
    speedOptions: List<Int>,
    currentSeed: Long,
    gameOver: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStep: () -> Unit,
    onSpeed: (Int) -> Unit,
    onReset: (seed: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appFont = rememberAppFontFamily()
    var seedText by remember { mutableStateOf(currentSeed.toString()) }
    var seedError by remember { mutableStateOf<String?>(null) }

    // Keep field in sync when controller seed changes (e.g. external reset).
    LaunchedEffect(currentSeed) {
        seedText = currentSeed.toString()
        seedError = null
    }

    fun parseSeedOrNull(): Long? {
        val raw = seedText.trim()
        if (raw.isEmpty()) {
            seedError = "請輸入 Seed"
            return null
        }
        val value = raw.toLongOrNull()
        if (value == null) {
            seedError = "Seed 須為整數"
            return null
        }
        seedError = null
        return value
    }

    fun resetWithField(resumePlay: Boolean) {
        val seed = parseSeedOrNull() ?: return
        onReset(seed)
        if (resumePlay) {
            onPlay()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Playback
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (playing) {
                Button(onClick = onPause) {
                    Text("暫停", fontFamily = appFont)
                }
            } else {
                Button(onClick = onPlay, enabled = !gameOver) {
                    Text("播放", fontFamily = appFont)
                }
            }
            OutlinedButton(onClick = onStep, enabled = !gameOver) {
                Text("單步", fontFamily = appFont)
            }
            OutlinedButton(onClick = { resetWithField(resumePlay = false) }) {
                Text("重置", fontFamily = appFont)
            }
        }

        // Speed
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("速度", style = MaterialTheme.typography.bodyMedium, fontFamily = appFont)
            speedOptions.forEach { option ->
                if (option == speed) {
                    Button(onClick = { onSpeed(option) }) {
                        Text("${option}×", fontFamily = appFont)
                    }
                } else {
                    OutlinedButton(onClick = { onSpeed(option) }) {
                        Text("${option}×", fontFamily = appFont)
                    }
                }
            }
        }

        // Seed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = seedText,
                onValueChange = { input ->
                    // Allow optional leading minus and digits only.
                    seedText = input.filterIndexed { index, c ->
                        c.isDigit() || (c == '-' && index == 0)
                    }.take(20)
                    seedError = null
                },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 120.dp),
                label = { Text("Seed", fontFamily = appFont) },
                singleLine = true,
                isError = seedError != null,
                supportingText = seedError?.let { { Text(it, fontFamily = appFont) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = appFont),
            )
            Button(
                onClick = { resetWithField(resumePlay = true) },
            ) {
                Text("套用 Seed", fontFamily = appFont)
            }
        }
    }
}
