package com.neojou.alsimugame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.SimSnapshot
import com.neojou.alsimugame.ui.theme.rememberAppFontFamily

/**
 * Persistent top HUD (GDD §6.3): calendar + camp food.
 *
 * - Year / day-in-year from absolute [SimSnapshot.day] and [SimConfig.DAYS_PER_YEAR]
 * - Day / night label (simple text icon)
 * - Large camp food number
 *
 * Values are read only from [snapshot] so they always match the simulation.
 */
@Composable
fun HudView(
    snapshot: SimSnapshot,
    modifier: Modifier = Modifier,
) {
    val appFont = rememberAppFontFamily()
    val calendar = rememberCalendar(snapshot.day)
    val phaseIcon = if (snapshot.isDay) "☀" else "☾"
    val phaseLabel = if (snapshot.isDay) "白天" else "夜晚"
    val hourLabel = "時辰 ${snapshot.hour}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: time
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "小小寨營",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = appFont,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                text = "第 ${calendar.year} 年 · 第 ${calendar.dayInYear} 日",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = appFont,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$phaseIcon $phaseLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = appFont,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = hourLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = appFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
                Text(
                    text = "（總第 ${snapshot.day} 日）",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = appFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }

        // Right: camp food (large)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "寨營糧食",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = appFont,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "糧",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = appFont,
                    color = FoodAccent,
                )
                Text(
                    text = "${snapshot.campFood}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFont,
                    color = FoodAccent,
                    lineHeight = 36.sp,
                )
            }
        }
    }
}

/** 1-based year and day-in-year derived from absolute simulation day. */
data class SimCalendar(
    val year: Int,
    val dayInYear: Int,
)

/**
 * Maps absolute [day] (0-based, from engine) to display calendar.
 * Year 1 starts at day 0; each year has [SimConfig.DAYS_PER_YEAR] days.
 */
fun calendarFromDay(
    day: Int,
    daysPerYear: Int = SimConfig.DAYS_PER_YEAR,
): SimCalendar {
    require(daysPerYear > 0)
    val safeDay = day.coerceAtLeast(0)
    val year = safeDay / daysPerYear + 1
    val dayInYear = safeDay % daysPerYear + 1
    return SimCalendar(year = year, dayInYear = dayInYear)
}

@Composable
private fun rememberCalendar(day: Int): SimCalendar =
    androidx.compose.runtime.remember(day) { calendarFromDay(day) }

private val FoodAccent = Color(0xFFD4A017)
