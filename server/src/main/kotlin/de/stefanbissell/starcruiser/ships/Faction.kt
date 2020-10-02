package de.stefanbissell.starcruiser.ships

enum class Faction {
    Player,
    Enemy,
    Neutral;

    infix fun isHostileTo(other: Faction): Boolean =
        enemies[this]?.contains(other) ?: false

    companion object {
        private val enemies = mapOf(
            Player to listOf(Enemy),
            Enemy to listOf(Player),
            Neutral to emptyList()
        )
    }
}
