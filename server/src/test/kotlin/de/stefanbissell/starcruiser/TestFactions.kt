package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.scenario.Faction

object TestFactions {

    val player = Faction(
        name = "player",
        enemies = listOf("enemy")
    )

    val enemy = Faction(
        name = "enemy",
        enemies = listOf("player")
    )

    val neutral = Faction(
        name = "neutral"
    )
}
