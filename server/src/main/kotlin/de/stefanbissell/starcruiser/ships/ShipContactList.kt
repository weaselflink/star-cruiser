package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ObjectId

class ShipContactList(
    val relativeTo: Ship,
    allShips: Map<ObjectId, Ship>,
) {

    constructor(
        relativeTo: Ship,
        list: List<Ship>
    ) : this(relativeTo, list.associateBy { it.id })

    val contacts: Map<ObjectId, ShipContact> = allShips
        .filterKeys { it != relativeTo.id }
        .mapValues { ShipContact(it.value) }
    val shipList
        get() = contacts.values.map { it.ship }

    operator fun get(key: ObjectId) = contacts[key]

    fun allInSensorRange() =
        contacts.values.filter { it.inSensorRange }

    inner class ShipContact(val ship: Ship) {
        val id = ship.id
        val position = ship.position
        val range by lazy {
            (position - relativeTo.position).length()
        }
        val inSensorRange by lazy {
            relativeTo.inSensorRange(ship)
        }
        val contactType = ship.getContactType(relativeTo)
    }
}
