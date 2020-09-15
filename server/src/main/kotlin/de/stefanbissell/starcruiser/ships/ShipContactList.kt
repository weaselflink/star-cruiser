package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.MapContactMessage
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScopeContactMessage

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

    fun outOfSensorRange(targetId: ObjectId) =
        this[targetId]?.let {
            !it.inSensorRange
        } ?: false

    inner class ShipContact(val ship: Ship) {
        val id = ship.id
        val designation = ship.designation
        val position = ship.position
        val rotation = ship.rotation
        val relativePosition by lazy {
            position - relativeTo.position
        }
        val range by lazy {
            relativePosition.length()
        }
        val inSensorRange by lazy {
            relativeTo.inSensorRange(ship)
        }
        val contactType = relativeTo.getContactType(ship)
        val scanLevel = relativeTo.getScanLevel(ship.id)

        fun toContactMessage() =
            ship.toContactMessage(relativeTo)

        fun toMapContactMessage() =
            MapContactMessage(
                id = id,
                type = contactType,
                designation = designation,
                position = position,
                rotation = rotation
            )

        fun toScopeContactMessage() =
            ScopeContactMessage(
                id = id,
                type = contactType,
                designation = designation,
                relativePosition = relativePosition,
                rotation = rotation,
                locked = relativeTo.isLocking(id)
            )
    }
}
