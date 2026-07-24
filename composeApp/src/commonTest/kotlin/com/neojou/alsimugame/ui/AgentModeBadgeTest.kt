package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.model.AgentMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AgentModeBadgeTest {

    @Test
    fun primaryModes_haveDistinctGlyphs() {
        val glyphs = PRIMARY_MODE_GLYPHS.values.toList()
        assertTrue(glyphs.size >= 4, "DoD requires at least 4 mode visuals")
        assertEquals(glyphs.size, glyphs.toSet().size, "mode glyphs must be unique: $glyphs")
    }

    @Test
    fun agentModeBadge_matchesPrimaryMap() {
        PRIMARY_MODE_GLYPHS.forEach { (mode, glyph) ->
            assertEquals(glyph, agentModeBadge(mode).glyph, "glyph for $mode")
        }
    }

    @Test
    fun workModes_pulse_restBobs_deadNeither() {
        assertTrue(agentModeBadge(AgentMode.TILLING).pulse)
        assertTrue(agentModeBadge(AgentMode.HARVESTING).pulse)
        assertTrue(agentModeBadge(AgentMode.RESTING).bob)
        assertTrue(!agentModeBadge(AgentMode.DEAD).pulse)
        assertTrue(!agentModeBadge(AgentMode.DEAD).bob)
    }

    @Test
    fun exploring_and_returning_differFromWork() {
        val explore = agentModeBadge(AgentMode.EXPLORING)
        val ret = agentModeBadge(AgentMode.RETURNING)
        val till = agentModeBadge(AgentMode.TILLING)
        assertNotEquals(explore.glyph, till.glyph)
        assertNotEquals(ret.glyph, till.glyph)
        assertNotEquals(explore.glyph, ret.glyph)
    }

    @Test
    fun allEnumModes_haveBadge() {
        AgentMode.entries.forEach { mode ->
            val badge = agentModeBadge(mode)
            assertEquals(mode, badge.mode)
            assertTrue(badge.glyph.isNotBlank())
        }
    }
}
