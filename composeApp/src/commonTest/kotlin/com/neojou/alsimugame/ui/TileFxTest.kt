package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.TileSnapshot
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileFxTest {

    private fun tile(
        state: TileState,
        pending: Int = 0,
    ) = TileSnapshot(x = 0, y = 0, state = state, ageDays = 0, pendingHarvest = pending)

    @Test
    fun harvestHighlight_onlyWhenFarmHasPending() {
        assertFalse(isHarvestHighlight(null))
        assertFalse(isHarvestHighlight(tile(TileState.GRASS)))
        assertFalse(isHarvestHighlight(tile(TileState.FARM, pending = 0)))
        assertFalse(isHarvestHighlight(tile(TileState.EMPTY)))
        assertTrue(isHarvestHighlight(tile(TileState.FARM, pending = 1)))
        assertTrue(isHarvestHighlight(tile(TileState.FARM, pending = 3)))
    }

    @Test
    fun workFx_onlyTillAndHarvestModes() {
        assertEquals(WorkFxKind.TILL, workFxKind(AgentMode.TILLING))
        assertEquals(WorkFxKind.HARVEST, workFxKind(AgentMode.HARVESTING))
        assertNull(workFxKind(AgentMode.EXPLORING))
        assertNull(workFxKind(AgentMode.RETURNING))
        assertNull(workFxKind(AgentMode.RESTING))
        assertNull(workFxKind(AgentMode.DEAD))
    }

    @Test
    fun workFxLabels_distinct() {
        assertEquals("墾", workFxLabel(WorkFxKind.TILL))
        assertEquals("收", workFxLabel(WorkFxKind.HARVEST))
    }
}
