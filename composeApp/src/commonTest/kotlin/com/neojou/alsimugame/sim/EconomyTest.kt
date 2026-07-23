package com.neojou.alsimugame.sim

import com.neojou.alsimugame.sim.model.Agent
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.GridPos
import com.neojou.alsimugame.sim.model.SimConfig
import com.neojou.alsimugame.sim.model.Tile
import com.neojou.alsimugame.sim.model.TileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EconomyTest {

    private fun agent(
        stamina: Int = SimConfig.MAX_STAMINA,
        carriedFood: Int = 0,
        mode: AgentMode = AgentMode.RESTING,
        pos: GridPos = GridPos.CAMP,
    ): Agent = Agent(
        id = "test-agent",
        gender = Gender.MALE,
        pos = pos,
        stamina = stamina,
        carriedFood = carriedFood,
        mode = mode,
    )

    // --- costs from SimConfig (no magic numbers) ---

    @Test
    fun actionCosts_matchSimConfig() {
        assertEquals(SimConfig.MOVE_STAMINA, Economy.moveCost().stamina)
        assertEquals(SimConfig.MOVE_HOURS, Economy.moveCost().hours)
        assertEquals(SimConfig.TILL_EXTRA_STAMINA, Economy.tillCost().stamina)
        assertEquals(SimConfig.TILL_HOURS, Economy.tillCost().hours)
        assertEquals(0, Economy.harvestCost().stamina)
        assertEquals(SimConfig.HARVEST_HOURS, Economy.harvestCost().hours)
    }

    // --- deposit ---

    @Test
    fun depositCarriedFood_addsToCampAndClearsCarry() {
        val a = agent(carriedFood = 5)
        val stock = Economy.depositCarriedFood(a, campFood = 10)
        assertEquals(15, stock)
        assertEquals(0, a.carriedFood)
    }

    @Test
    fun depositCarriedFood_zeroCarry_leavesStockUnchanged() {
        val a = agent(carriedFood = 0)
        assertEquals(7, Economy.depositCarriedFood(a, campFood = 7))
    }

    // --- supply stamina ---

    @Test
    fun supplyStaminaFromCamp_fullRestore_fromZeroUsesFourFood() {
        // need 10 stamina; 1 food = 3 stamina → 4 food units (3*3=9, +1 food for last 1)
        val a = agent(stamina = 0)
        val remaining = Economy.supplyStaminaFromCamp(a, campFood = 10)
        assertEquals(SimConfig.MAX_STAMINA, a.stamina)
        assertEquals(10 - 4, remaining)
        assertEquals(4, Economy.foodUnitsNeededToFill(agent(stamina = 0)))
    }

    @Test
    fun supplyStaminaFromCamp_alreadyFull_consumesNoFood() {
        val a = agent(stamina = SimConfig.MAX_STAMINA)
        val remaining = Economy.supplyStaminaFromCamp(a, campFood = 5)
        assertEquals(SimConfig.MAX_STAMINA, a.stamina)
        assertEquals(5, remaining)
        assertEquals(0, Economy.foodUnitsNeededToFill(a))
    }

    @Test
    fun supplyStaminaFromCamp_zeroFood_partialOrNoRestore() {
        val a = agent(stamina = 2)
        val remaining = Economy.supplyStaminaFromCamp(a, campFood = 0)
        assertEquals(0, remaining)
        assertEquals(2, a.stamina)
    }

    @Test
    fun supplyStaminaFromCamp_scarceFood_partialRestore() {
        val a = agent(stamina = 0)
        // 1 food → +3 stamina only
        val remaining = Economy.supplyStaminaFromCamp(a, campFood = 1)
        assertEquals(0, remaining)
        assertEquals(SimConfig.FOOD_TO_STAMINA, a.stamina)
    }

    @Test
    fun supplyStaminaFromCamp_doesNotExceedMax() {
        val a = agent(stamina = 9)
        // 1 food would give +3 but cap at 10
        val remaining = Economy.supplyStaminaFromCamp(a, campFood = 5)
        assertEquals(SimConfig.MAX_STAMINA, a.stamina)
        assertEquals(4, remaining)
    }

    // --- move / till spend ---

    @Test
    fun tryPayMove_deductsMoveStamina() {
        val a = agent(stamina = 5)
        assertTrue(Economy.tryPayMove(a))
        assertEquals(5 - SimConfig.MOVE_STAMINA, a.stamina)
    }

    @Test
    fun tryPayMove_failsWhenStaminaInsufficient() {
        val a = agent(stamina = 0)
        assertFalse(Economy.tryPayMove(a))
        assertEquals(0, a.stamina)
    }

    @Test
    fun tryPayAndTill_spendsTillCostAndConvertsGrass() {
        val a = agent(stamina = 5)
        val tile = Tile(state = TileState.GRASS)
        assertTrue(Economy.tryPayAndTill(a, tile))
        assertEquals(5 - SimConfig.TILL_EXTRA_STAMINA, a.stamina)
        assertEquals(TileState.FARM, tile.state)
        assertEquals(0, tile.ageDays)
    }

    @Test
    fun tryPayAndTill_failsWithoutSpending_whenNoStamina() {
        val a = agent(stamina = 0)
        val tile = Tile(state = TileState.GRASS)
        assertFalse(Economy.tryPayAndTill(a, tile))
        assertEquals(0, a.stamina)
        assertEquals(TileState.GRASS, tile.state)
    }

    // --- harvest ---

    @Test
    fun harvest_movesPendingToCarriedAndClearsTile() {
        val a = agent(carriedFood = 2)
        val tile = Tile(state = TileState.FARM, pendingHarvest = 4)
        val amount = Economy.harvest(a, tile)
        assertEquals(4, amount)
        assertEquals(6, a.carriedFood)
        assertEquals(0, tile.pendingHarvest)
        assertEquals(TileState.FARM, tile.state)
    }

    @Test
    fun harvest_onNonFarm_throws() {
        val a = agent()
        assertFailsWith<IllegalArgumentException> {
            Economy.harvest(a, Tile(state = TileState.GRASS, pendingHarvest = 3))
        }
    }

    @Test
    fun tryHarvest_returnsNullOnGrass_andAmountOnFarm() {
        val a = agent()
        assertNull(Economy.tryHarvest(a, Tile(state = TileState.EMPTY)))
        assertEquals(0, a.carriedFood)

        val farm = Tile(state = TileState.FARM, pendingHarvest = 2)
        assertEquals(2, Economy.tryHarvest(a, farm))
        assertEquals(2, a.carriedFood)
    }

    @Test
    fun harvest_doesNotRequireStamina() {
        val a = agent(stamina = 0)
        val tile = Tile(state = TileState.FARM, pendingHarvest = 1)
        assertEquals(1, Economy.harvest(a, tile))
        assertEquals(0, a.stamina)
        assertEquals(1, a.carriedFood)
    }

    // --- night rest ---

    @Test
    fun applyNightRest_atCamp_restoresConfiguredAmount() {
        val a = agent(stamina = 4, pos = GridPos.CAMP)
        Economy.applyNightRest(a)
        assertEquals(4 + SimConfig.NIGHT_REST_STAMINA, a.stamina)
    }

    @Test
    fun applyNightRest_notAtCamp_noChange() {
        val a = agent(stamina = 4, pos = GridPos(0, 0))
        Economy.applyNightRest(a)
        assertEquals(4, a.stamina)
    }

    @Test
    fun deadAgent_cannotSpendOrSupply() {
        val a = agent(stamina = 5, mode = AgentMode.DEAD)
        assertFalse(Economy.canAfford(a, Economy.moveCost()))
        assertFalse(Economy.tryPayMove(a))
        assertEquals(5, a.stamina)
        assertEquals(3, Economy.supplyStaminaFromCamp(a, campFood = 3))
        assertEquals(5, a.stamina)
    }
}
