package com.neojou.alsimugame.ui

/**
 * Result of parsing a seed text field (Settings dialog / legacy controls).
 */
sealed class SeedParseResult {
    data class Ok(val seed: Long) : SeedParseResult()
    data class Err(val message: String) : SeedParseResult()
}

/**
 * Parses user seed input: optional leading minus, integer only.
 */
fun parseSeedInput(raw: String): SeedParseResult {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return SeedParseResult.Err("請輸入 Seed")
    val value = trimmed.toLongOrNull()
        ?: return SeedParseResult.Err("Seed 須為整數")
    return SeedParseResult.Ok(value)
}

/** Filter keystrokes for the seed field: digits and optional leading '-'. */
fun filterSeedFieldInput(input: String, maxLen: Int = 20): String =
    input.filterIndexed { index, c ->
        c.isDigit() || (c == '-' && index == 0)
    }.take(maxLen)
