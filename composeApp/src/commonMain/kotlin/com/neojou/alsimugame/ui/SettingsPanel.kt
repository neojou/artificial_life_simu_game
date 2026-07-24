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
 * Speed + Seed controls for the Settings dialog (Vis-C).
 *
 * Playback (play / step / reset) lives on the top menu bar.
 */
@Composable
fun SettingsPanel(
    speed: Int,
    speedOptions: List<Int>,
    currentSeed: Long,
    onSpeed: (Int) -> Unit,
    onApplySeed: (seed: Long, resumePlay: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appFont = rememberAppFontFamily()
    var seedText by remember { mutableStateOf(currentSeed.toString()) }
    var seedError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentSeed) {
        seedText = currentSeed.toString()
        seedError = null
    }

    fun applySeed(resumePlay: Boolean) {
        when (val result = parseSeedInput(seedText)) {
            is SeedParseResult.Ok -> {
                seedError = null
                onApplySeed(result.seed, resumePlay)
            }
            is SeedParseResult.Err -> seedError = result.message
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "模擬設定",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = appFont,
        )

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = seedText,
                onValueChange = { input ->
                    seedText = filterSeedFieldInput(input)
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
            Button(onClick = { applySeed(resumePlay = true) }) {
                Text("套用 Seed", fontFamily = appFont)
            }
        }

        Text(
            text = "「套用 Seed」會以該 seed 重置並繼續播放。選單「重置」則用目前 seed 重置並暫停。",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = appFont,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
    }
}
