package com.neojou.alsimugame.sim.model

import kotlinx.serialization.Serializable

/**
 * Villager gender presentation for v0.1 (two fixed adults; no reproduction).
 */
@Serializable
enum class Gender {
    MALE,
    FEMALE,
}
