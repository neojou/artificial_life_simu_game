package com.neojou.alsimugame.ui

import com.neojou.alsimugame.composeapp.generated.resources.Res
import com.neojou.alsimugame.composeapp.generated.resources.pawn_female_carry
import com.neojou.alsimugame.composeapp.generated.resources.pawn_female_idle
import com.neojou.alsimugame.composeapp.generated.resources.pawn_female_work
import com.neojou.alsimugame.composeapp.generated.resources.pawn_male_carry
import com.neojou.alsimugame.composeapp.generated.resources.pawn_male_idle
import com.neojou.alsimugame.composeapp.generated.resources.pawn_male_work
import com.neojou.alsimugame.composeapp.generated.resources.tile_camp
import com.neojou.alsimugame.composeapp.generated.resources.tile_empty
import com.neojou.alsimugame.composeapp.generated.resources.tile_farm
import com.neojou.alsimugame.composeapp.generated.resources.tile_farm_ripe
import com.neojou.alsimugame.composeapp.generated.resources.tile_grass
import com.neojou.alsimugame.sim.model.AgentMode
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.Gender
import com.neojou.alsimugame.sim.model.TileSnapshot
import com.neojou.alsimugame.sim.model.TileState
import org.jetbrains.compose.resources.DrawableResource

/**
 * Maps simulation state → drawable resources (top-down RimWorld-style art).
 */
object WorldAssets {
    fun tileFor(isCamp: Boolean, tile: TileSnapshot?): DrawableResource {
        if (isCamp) return Res.drawable.tile_camp
        if (tile == null) return Res.drawable.tile_empty
        return when (tile.state) {
            TileState.GRASS -> Res.drawable.tile_grass
            TileState.EMPTY -> Res.drawable.tile_empty
            TileState.FARM ->
                if (tile.pendingHarvest > 0) Res.drawable.tile_farm_ripe
                else Res.drawable.tile_farm
        }
    }

    /**
     * Pose sprite by mode / carried food (M5-T2: badge still distinguishes modes
     * that share the same pose sheet).
     */
    fun pawnFor(agent: AgentSnapshot): DrawableResource {
        val male = agent.gender == Gender.MALE
        return when (agent.mode) {
            AgentMode.DEAD ->
                if (male) Res.drawable.pawn_male_idle else Res.drawable.pawn_female_idle
            AgentMode.TILLING, AgentMode.HARVESTING ->
                if (male) Res.drawable.pawn_male_work else Res.drawable.pawn_female_work
            AgentMode.RETURNING, AgentMode.SUPPLYING ->
                if (agent.carriedFood > 0) {
                    if (male) Res.drawable.pawn_male_carry else Res.drawable.pawn_female_carry
                } else {
                    if (male) Res.drawable.pawn_male_idle else Res.drawable.pawn_female_idle
                }
            AgentMode.RESTING, AgentMode.EXPLORING ->
                if (agent.carriedFood > 0) {
                    if (male) Res.drawable.pawn_male_carry else Res.drawable.pawn_female_carry
                } else {
                    if (male) Res.drawable.pawn_male_idle else Res.drawable.pawn_female_idle
                }
        }
    }
}
