package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ObjectId

class ShipContactList(
    val relativeTo: Ship,
    allShips: Map<ObjectId, Ship>,
) {

    val contacts: Map<ObjectId, ShipContact> = allShips
        .filterKeys { it != relativeTo.id }
        .mapValues { ShipContact(it.value) }
    val shipList
        get() = contacts.values.map { it.ship }

    operator fun get(key: ObjectId) = contacts[key]

    inner class ShipContact(val ship: Ship) {
        val range by lazy {
            (ship.position - relativeTo.position).length()
        }
    }
}
