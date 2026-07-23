package com.neojou.alsimugame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.neojou.alsimugame.composeapp.generated.resources.NotoSansTC
import com.neojou.alsimugame.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

/**
 * App-wide theme with a CJK-capable font family.
 *
 * Desktop can fall back to system Chinese fonts; Wasm/Skiko cannot — it needs
 * an embedded TTF/OTF. [NotoSansTC] covers Traditional Chinese UI text.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val fontFamily = rememberAppFontFamily()
    // Read LocalTypography outside remember (it is @Composable).
    val baseTypography = MaterialTheme.typography
    val typography = remember(fontFamily, baseTypography) {
        baseTypography.withFontFamily(fontFamily)
    }
    MaterialTheme(
        typography = typography,
        content = content,
    )
}

@Composable
fun rememberAppFontFamily(): FontFamily {
    val regular = Font(Res.font.NotoSansTC, weight = FontWeight.Normal)
    return remember(regular) {
        FontFamily(regular)
    }
}

/**
 * Applies [fontFamily] to every Material 3 text style so default Text
 * and custom styles inherit CJK glyphs (needed on Wasm).
 */
private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)
