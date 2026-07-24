package com.neojou.alsimugame.ui

import com.neojou.alsimugame.composeapp.generated.resources.Res
import com.neojou.alsimugame.composeapp.generated.resources.pawn_female_carry
import com.neojou.alsimugame.composeapp.generated.resources.pawn_female_idle
import com.neojou.alsimugame.composeapp.generated.resources.pawn_female_work
import com.neojou.alsimugame.composeapp.generated.resources.pawn_male_carry
import com.neojou.alsimugame.composeapp.generated.resources.pawn_male_idle
import com.neojou.alsimugame.composeapp.generated.resources.pawn_male_work
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender
import kotlin.test.Test
import kotlin.test.assertEquals

class WorldAssetsPawnTest {

    private fun agent(
        gender: Gender,
        mode: AgentMode,
        carried: Int = 0,
    ) = AgentSnapshot(
        id = "t",
        gender = gender,
        x = 0,
        y = 0,
        stamina = 10,
        carriedFood = carried,
        ageDays = 0,
        mode = mode,
        returnHome = false,
    )

    @Test
    fun tilling_usesWorkPose_byGender() {
        assertEquals(Res.drawable.pawn_male_work, WorldAssets.pawnFor(agent(Gender.MALE, AgentMode.TILLING)))
        assertEquals(Res.drawable.pawn_female_work, WorldAssets.pawnFor(agent(Gender.FEMALE, AgentMode.HARVESTING)))
    }

    @Test
    fun exploringIdle_vsCarry() {
        assertEquals(Res.drawable.pawn_male_idle, WorldAssets.pawnFor(agent(Gender.MALE, AgentMode.EXPLORING, 0)))
        assertEquals(Res.drawable.pawn_male_carry, WorldAssets.pawnFor(agent(Gender.MALE, AgentMode.EXPLORING, 2)))
        assertEquals(Res.drawable.pawn_female_idle, WorldAssets.pawnFor(agent(Gender.FEMALE, AgentMode.RESTING, 0)))
    }

    @Test
    fun returningWithFood_usesCarry() {
        assertEquals(
            Res.drawable.pawn_female_carry,
            WorldAssets.pawnFor(agent(Gender.FEMALE, AgentMode.RETURNING, 1)),
        )
    }
}
