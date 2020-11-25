package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.MapContactMessage
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScopeContactMessage
import de.stefanbissell.starcruiser.ShipType
import de.stefanbissell.starcruiser.ships.combat.Torpedo

class ContactList(
    val relativeTo: Ship,
    allShips: Map<ObjectId, Ship> = emptyMap(),
    allTorpedoes: Map<ObjectId, Torpedo> = emptyMap()
) {

    constructor(
        relativeTo: Ship,
        list: List<Ship>
    ) : this(relativeTo, list.associateBy { it.id })

    val contacts: Map<ObjectId, Contact> = (allShips + allTorpedoes)
        .filterKeys { it != relativeTo.id }
        .mapValues { Contact(it.value) }

    operator fun get(key: ObjectId) = contacts[key]

    fun allInSensorRange() =
        contacts.values.filter { it.inSensorRange }

    fun allNearScopeRange() =
        contacts.values.filter { it.nearScopeRange }

    fun outOfSensorRange(targetId: ObjectId) =
        this[targetId]?.let {
            !it.inSensorRange
        } ?: false

    inner class Contact(val dynamicObject: DynamicObject) {
        val id = dynamicObject.id
        val designation = dynamicObject.designation
        val position = dynamicObject.position
        val rotation = dynamicObject.rotation
        val speed = dynamicObject.speed
        val relativePosition by lazy {
            position - relativeTo.position
        }
        val range by lazy {
            relativePosition.length()
        }
        val inSensorRange by lazy {
            relativeTo.inSensorRange(dynamicObject)
        }
        val nearScopeRange by lazy {
            range <= relativeTo.template.shortRangeScopeRange * 1.1
        }
        val shipType = if (dynamicObject is Ship) ShipType.Vessel else ShipType.Projectile
        val contactType = relativeTo.getContactType(dynamicObject)
        val isEnemy = contactType == ContactType.Enemy
        val scanLevel = relativeTo.getScanLevel(dynamicObject.id)

        fun toContactMessage() =
            if (dynamicObject is Ship) {
                dynamicObject.toContactMessage(relativeTo)
            } else {
                null
            }

        fun toMapContactMessage() =
            MapContactMessage(
                id = id,
                shipType = shipType,
                type = contactType,
                designation = designation,
                position = position,
                rotation = rotation
            )

        fun toScopeContactMessage() =
            ScopeContactMessage(
                id = id,
                shipType = shipType,
                type = contactType,
                designation = designation,
                relativePosition = relativePosition,
                rotation = rotation,
                locked = relativeTo.isLocking(id)
            )
    }
}

fun List<ContactList.Contact>.onlyVessels() =
    filter { it.shipType == ShipType.Vessel }

fun List<ContactList.Contact>.onlyEnemies() =
    filter { it.contactType == ContactType.Enemy }
