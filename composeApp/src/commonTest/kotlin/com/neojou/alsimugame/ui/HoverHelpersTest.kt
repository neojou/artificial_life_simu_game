package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HoverHelpersTest {

    @Test
    fun daysUntilLandTransition_farmAndEmpty() {
        assertEquals(12, daysUntilLandTransition(TileState.FARM, 0))
        assertEquals(5, daysUntilLandTransition(TileState.EMPTY, 7))
        assertEquals(0, daysUntilLandTransition(TileState.FARM, SimConfig.LAND_STATE_DAYS))
        assertNull(daysUntilLandTransition(TileState.GRASS, 3))
    }

    @Test
    fun agentModeLabels_coverAll() {
        assertEquals("探索中", agentModeLabel(AgentMode.EXPLORING))
        assertEquals("正在開墾", agentModeLabel(AgentMode.TILLING))
        assertEquals("返回寨營中", agentModeLabel(AgentMode.RETURNING))
        assertEquals("已死亡", agentModeLabel(AgentMode.DEAD))
    }
}
