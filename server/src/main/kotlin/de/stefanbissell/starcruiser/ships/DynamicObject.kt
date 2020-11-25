package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ShipType
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.scenario.Faction
import de.stefanbissell.starcruiser.ships.combat.DamageEvent

interface DynamicObject {

    val id: ObjectId
    val shipType: ShipType
    val faction: Faction
    val designation: String
    val position: Vector2
    val rotation: Double
    val speed: Vector2

    fun applyDamage(damageEvent: DamageEvent)
}
