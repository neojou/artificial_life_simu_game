package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards GDD §11 parameter table against accidental drift.
 */
class SimConfigTest {

    @Test
    fun gddSection11_coreParameters() {
        assertEquals(10, SimConfig.MAX_STAMINA)
        assertEquals(3, SimConfig.FOOD_TO_STAMINA)
        assertEquals(1, SimConfig.FARM_YIELD_PER_DAY)
        assertEquals(3, SimConfig.MAX_PENDING_HARVEST)
        assertEquals(1, SimConfig.MOVE_STAMINA)
        assertEquals(1, SimConfig.MOVE_HOURS)
        assertEquals(1, SimConfig.TILL_EXTRA_STAMINA)
        assertEquals(1, SimConfig.TILL_HOURS)
        assertEquals(1, SimConfig.HARVEST_HOURS)
        assertEquals(12, SimConfig.LAND_STATE_DAYS)
        assertEquals(10, SimConfig.INITIAL_CAMP_FOOD)
        assertEquals(12, SimConfig.DAYS_PER_YEAR)
        assertEquals(60, SimConfig.LIFESPAN_DAYS)
        assertEquals(6, SimConfig.HOURS_PER_DAY)
    }

    @Test
    fun mapLayoutConstants() {
        assertEquals(5, SimConfig.GRID_SIZE)
        assertEquals(2, SimConfig.CAMP_X)
        assertEquals(2, SimConfig.CAMP_Y)
    }
}
