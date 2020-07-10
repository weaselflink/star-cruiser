package de.stefanbissell.starcruiser

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PrimitiveDescriptor
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val jsonConfiguration = JsonConfiguration.Stable.copy(
    encodeDefaults = true,
    isLenient = true,
    prettyPrint = true
)

interface Identifiable {
    val id: ObjectId
}

interface Positional {
    val position: Vector2
}

@Serializable
data class ObjectId(val id: String) {

    @Serializer(forClass = ObjectId::class)
    companion object : KSerializer<ObjectId> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor("ShipId", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ObjectId) {
            encoder.encodeString(value.id)
        }

        override fun deserialize(decoder: Decoder): ObjectId {
            return ObjectId(decoder.decodeString())
        }

        fun random() = ObjectId(Uuid().toString())
    }
}

@Serializable
data class GameStateMessage(
    val counter: Long,
    val snapshot: SnapshotMessage
) {
    fun toJson(): String = Json(jsonConfiguration).stringify(serializer(), this)

    companion object {
        fun parse(input: String): GameStateMessage = Json(jsonConfiguration).parse(serializer(), input)
    }
}

@Serializable
sealed class SnapshotMessage {

    interface ShipSnapshot {
        val ship: ShipMessage
    }

    interface ShortRangeScopeStation {
        val ship: ShipMessage
        val contacts: List<ScopeContactMessage>
        val asteroids: List<AsteroidMessage>
    }

    @Serializable
    data class ShipSelection(
        val playerShips: List<PlayerShipMessage>
    ) : SnapshotMessage()

    @Serializable
    object ShipDestroyed : SnapshotMessage()

    @Serializable
    data class Helm(
        override val ship: ShipMessage,
        override val contacts: List<ScopeContactMessage>,
        override val asteroids: List<AsteroidMessage>
    ) : SnapshotMessage(), ShipSnapshot, ShortRangeScopeStation

    @Serializable
    data class Weapons(
        override val ship: ShipMessage,
        override val contacts: List<ScopeContactMessage>,
        override val asteroids: List<AsteroidMessage>
    ) : SnapshotMessage(), ShipSnapshot, ShortRangeScopeStation

    @Serializable
    data class Navigation(
        override val ship: ShipMessage,
        val contacts: List<ContactMessage>,
        val asteroids: List<AsteroidMessage>
    ) : SnapshotMessage(), ShipSnapshot

    @Serializable
    data class MainScreen(
        override val ship: ShipMessage,
        val longRangeContacts: List<ContactMessage>,
        override val contacts: List<ScopeContactMessage>,
        override val asteroids: List<AsteroidMessage>
    ) : SnapshotMessage(), ShipSnapshot, ShortRangeScopeStation
}

@Serializable
data class PlayerShipMessage(
    val id: ObjectId,
    val name: String,
    val shipClass: String?
)

@Serializable
data class ShipMessage(
    val id: ObjectId,
    val designation: String,
    val shipClass: String?,
    val position: Vector2,
    val speed: Vector2,
    val rotation: Double,
    val heading: Double,
    val velocity: Double,
    val throttle: Int,
    val thrust: Double,
    val rudder: Int,
    val history: List<Pair<Double, Vector2>>,
    val shortRangeScopeRange: Double,
    val waypoints: List<WaypointMessage>,
    val scanProgress: ScanProgress?,
    val lockProgress: LockStatus,
    val beams: List<BeamMessage>,
    val shield: ShieldMessage,
    val hull: Double,
    val hullMax: Double,
    val jumpDrive: JumpDriveMessage
)

@Serializable
data class ContactMessage(
    override val id: ObjectId,
    val type: ContactType,
    val scanLevel: ScanLevel,
    val designation: String,
    override val position: Vector2,
    val relativePosition: Vector2,
    val rotation: Double,
    val bearing: Double,
    val beams: List<BeamMessage>,
    val shield: ShieldMessage,
    val jumpAnimation: Double?
) : Identifiable, Positional

@Serializable
data class AsteroidMessage(
    override val id: ObjectId,
    val radius: Double,
    override val position: Vector2,
    val relativePosition: Vector2,
    val rotation: Double
) : Identifiable, Positional

@Serializable
data class ScopeContactMessage(
    val id: ObjectId,
    val type: ContactType,
    val designation: String,
    val relativePosition: Vector2,
    val rotation: Double,
    val locked: Boolean
)

enum class ContactType {
    Unknown,
    Friendly
}

enum class ScanLevel {
    None,
    Faction;

    fun next() =
        when (this) {
            None -> Faction
            Faction -> Faction
        }

    companion object {
        val highest = Faction
    }
}

@Serializable
data class ScanProgress(
    val targetId: ObjectId,
    val progress: Double
)

@Serializable
sealed class LockStatus {

    interface LockedTarget {
        val targetId: ObjectId
    }

    @Serializable
    object NoLock : LockStatus()

    @Serializable
    data class InProgress(
        override val targetId: ObjectId,
        val progress: Double
    ) : LockStatus(), LockedTarget

    @Serializable
    data class Locked(
        override val targetId: ObjectId
    ) : LockStatus(), LockedTarget
}

@Serializable
data class WaypointMessage(
    val index: Int,
    val name: String,
    override val position: Vector2,
    val relativePosition: Vector2,
    val bearing: Double
) : Positional

@Serializable
data class BeamMessage(
    val position: Vector3 = Vector3(),
    val minRange: Double,
    val maxRange: Double,
    val leftArc: Double,
    val rightArc: Double,
    val status: BeamStatus,
    val targetId: ObjectId?
)

@Serializable
sealed class BeamStatus {

    @Serializable
    object Idle : BeamStatus()

    @Serializable
    data class Recharging(val progress: Double = 0.0) : BeamStatus() {
        fun update(change: Double) = copy(progress = progress + change)
    }

    @Serializable
    data class Firing(
        val progress: Double = 0.0
    ) : BeamStatus() {
        fun update(change: Double) = copy(progress = progress + change)
    }
}

@Serializable
data class ShieldMessage(
    val radius: Double,
    val up: Boolean,
    val activated: Boolean,
    val strength: Double,
    val max: Double
)

@Serializable
sealed class JumpDriveMessage {

    abstract val ratio: Double
    abstract val distance: Int
    abstract val animation: Double?

    @Serializable
    data class Ready(
        override val ratio: Double,
        override val distance: Int,
        override val animation: Double?
    ) : JumpDriveMessage()

    @Serializable
    data class Jumping(
        override val ratio: Double,
        override val distance: Int,
        override val animation: Double?,
        val progress: Double
    ) : JumpDriveMessage()

    @Serializable
    data class Recharging(
        override val ratio: Double,
        override val distance: Int,
        override val animation: Double?,
        val progress: Double
    ) : JumpDriveMessage()
}
