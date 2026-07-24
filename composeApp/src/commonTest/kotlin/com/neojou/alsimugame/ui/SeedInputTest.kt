package com.neojou.alsimugame.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SeedInputTest {

    @Test
    fun parse_validInteger() {
        val r = parseSeedInput("42")
        assertIs<SeedParseResult.Ok>(r)
        assertEquals(42L, r.seed)
    }

    @Test
    fun parse_negativeAndTrim() {
        val r = parseSeedInput("  -7  ")
        assertIs<SeedParseResult.Ok>(r)
        assertEquals(-7L, r.seed)
    }

    @Test
    fun parse_empty_errors() {
        val r = parseSeedInput("   ")
        assertIs<SeedParseResult.Err>(r)
        assertTrue(r.message.contains("Seed"))
    }

    @Test
    fun parse_nonInteger_errors() {
        assertIs<SeedParseResult.Err>(parseSeedInput("12.5"))
        assertIs<SeedParseResult.Err>(parseSeedInput("abc"))
    }

    @Test
    fun filter_allowsLeadingMinusAndDigits() {
        assertEquals("-12", filterSeedFieldInput("-12ab"))
        assertEquals("12", filterSeedFieldInput("1a2"))
        assertEquals("12345678901234567890", filterSeedFieldInput("1234567890123456789012345", maxLen = 20))
    }
}
