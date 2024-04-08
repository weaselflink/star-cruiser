package de.stefanbissell.starcruiser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.random.Random

val configuredJson = Json {
    prettyPrint = true
}

interface Identifiable {
    val id: ObjectId
}

interface IdentifiableWithModel : Identifiable {
    val model: String
}

interface Positional {
    val position: Vector2
}

@Serializable(with = ObjectIdAsStringSerializer::class)
data class ObjectId(val id: String) {

    companion object {
        fun random() = ObjectId(Uuid().toString())
    }
}

object ObjectIdAsStringSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ShipId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return ObjectId(decoder.decodeString())
    }

    fun random() = ObjectId(Uuid().toString())
}

@Serializable
data class GameStateMessage(
    val counter: Long,
    val snapshot: SnapshotMessage
) {
    fun toJson(): String = configuredJson.encodeToString(serializer(), this)

    companion object {
        fun parse(input: String): GameStateMessage = configuredJson.decodeFromString(serializer(), input)
    }
}

@Serializable
sealed class SnapshotMessage {

    interface CrewSnapshot

    interface ShortRangeScopeStation {
        val shortRangeScope: ShortRangeScopeMessage
        val contacts: List<ScopeContactMessage>
        val asteroids: List<ScopeAsteroidMessage>
    }

    @Serializable
    data class ShipSelection(
        val playerShips: List<PlayerShipMessage>
    ) : SnapshotMessage()

    @Serializable
    object ShipDestroyed : SnapshotMessage()

    @Serializable
    data class Helm(
        override val shortRangeScope: ShortRangeScopeMessage,
        override val contacts: List<ScopeContactMessage>,
        override val asteroids: List<ScopeAsteroidMessage>,
        val throttle: Int,
        val rudder: Int,
        val jumpDrive: JumpDriveMessage
    ) : SnapshotMessage(), ShortRangeScopeStation, CrewSnapshot

    @Serializable
    data class Weapons(
        override val shortRangeScope: ShortRangeScopeMessage,
        override val contacts: List<ScopeContactMessage>,
        override val asteroids: List<ScopeAsteroidMessage>,
        val hull: Double,
        val hullMax: Double,
        val shield: ShieldMessage,
        val tubes: TubesMessage
    ) : SnapshotMessage(), ShortRangeScopeStation, CrewSnapshot

    @Serializable
    data class Navigation(
        val ship: NavigationShipMessage,
        val mapSelection: MapSelectionMessage?,
        val contacts: List<MapContactMessage>,
        val asteroids: List<MapAsteroidMessage>,
        val mapAreas: List<MapAreaMessage>
    ) : SnapshotMessage(), CrewSnapshot

    @Serializable
    data class Engineering(
        val powerSettings: PowerMessage
    ) : SnapshotMessage(), CrewSnapshot

    interface MainScreen : CrewSnapshot

    @Serializable
    data class MainScreen3d(
        val ship: ShipMessage,
        val contacts: List<ContactMessage>,
        val asteroids: List<AsteroidMessage>
    ) : SnapshotMessage(), MainScreen

    @Serializable
    data class MainScreenShortRangeScope(
        override val shortRangeScope: ShortRangeScopeMessage,
        override val contacts: List<ScopeContactMessage>,
        override val asteroids: List<ScopeAsteroidMessage>
    ) : SnapshotMessage(), ShortRangeScopeStation, MainScreen
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
    val model: String,
    val designation: String,
    val position: Vector2,
    val rotation: Double,
    val beams: BeamsMessage,
    val shield: ShieldMessage,
    val jumpDrive: JumpDriveMessage,
    val mainScreenView: MainScreenView,
    val frontCamera: CameraMessage,
    val leftCamera: CameraMessage,
    val rightCamera: CameraMessage,
    val rearCamera: CameraMessage
)

@Serializable
data class NavigationShipMessage(
    val id: ObjectId,
    val position: Vector2,
    val rotation: Double,
    val history: List<Vector2>,
    val waypoints: List<WaypointMessage>,
    val sensorRange: Double,
    val scanProgress: ScanProgressMessage?
)

@Serializable
data class ShortRangeScopeMessage(
    val shortRangeScopeRange: Double,
    val rotation: Double,
    val history: List<Vector2>,
    val waypoints: List<WaypointMessage>,
    val lockProgress: LockStatus,
    val beams: BeamsMessage,
    val tubes: List<TubeDirectionMessage>
)

@Serializable
data class ContactMessage(
    override val id: ObjectId,
    override val model: String,
    override val position: Vector2,
    val relativePosition: Vector2,
    val rotation: Double,
    val beams: List<BeamMessage>,
    val shield: ContactShieldMessage,
    val jumpAnimation: Double?
) : IdentifiableWithModel, Positional

@Serializable
data class MapContactMessage(
    val id: ObjectId,
    val shipType: ShipType = ShipType.Vessel,
    val type: ContactType,
    val designation: String,
    override val position: Vector2,
    val rotation: Double
) : Positional

@Serializable
data class ScopeContactMessage(
    val id: ObjectId,
    val shipType: ShipType = ShipType.Vessel,
    val type: ContactType,
    val designation: String,
    val relativePosition: Vector2,
    val rotation: Double,
    val locked: Boolean
)

@Serializable
data class AsteroidMessage(
    override val id: ObjectId,
    override val model: String,
    val radius: Double,
    override val position: Vector2,
    val relativePosition: Vector2,
    val rotation: Double
) : IdentifiableWithModel, Positional

@Serializable
data class ScopeAsteroidMessage(
    override val id: ObjectId,
    val radius: Double,
    val relativePosition: Vector2,
    val rotation: Double
) : Identifiable

@Serializable
data class MapAsteroidMessage(
    override val id: ObjectId,
    val radius: Double,
    override val position: Vector2,
    val rotation: Double
) : Identifiable, Positional

@Serializable
data class CameraMessage(
    val fov: Double,
    val position: Vector3,
    val rotation: Double
)

enum class MainScreenView {
    Front,
    Left,
    Right,
    Rear,
    Top,
    Scope;

    val next: MainScreenView
        get() = when (this) {
            Front -> Left
            Left -> Rear
            Rear -> Right
            Right -> Top
            Top -> Scope
            Scope -> Front
        }
}

enum class ShipType {
    Vessel,
    Projectile
}

enum class ContactType {
    Unknown,
    Friendly,
    Enemy,
    Neutral
}

enum class ScanLevel {
    None,
    Basic,
    Detailed;

    val canBeIncreased
        get() = this != highest

    val next
        get() =
            when (this) {
                None -> Basic
                Basic -> Detailed
                Detailed -> Detailed
            }

    companion object {
        private val highest = Detailed
    }
}

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
    val relativePosition: Vector2
) : Positional

@Serializable
data class MapSelectionMessage(
    val position: Vector2,
    val label: String,
    val bearing: Int,
    val range: Int,
    val hullRatio: Double? = null,
    val shield: SimpleShieldMessage? = null,
    val shieldModulation: Int? = null,
    val beamModulation: Int? = null,
    val systemsDamage: Map<PoweredSystemType, Double>? = null,
    val canScan: Boolean = false,
    val canDelete: Boolean = false
)

@Serializable
data class SimpleShieldMessage(
    val up: Boolean,
    val ratio: Double
)

@Serializable
data class BeamsMessage(
    val modulation: Int,
    val beams: List<BeamMessage>
) : AbstractList<BeamMessage>() {

    override val size: Int
        get() = beams.size

    override fun get(index: Int): BeamMessage =
        beams[index]
}

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
data class TubeDirectionMessage(
    val position: Vector3 = Vector3(),
    val rotation: Double
)

@Serializable
data class TubesMessage(
    val magazineMax: Int,
    val magazineRemaining: Int,
    val tubes: List<TubeMessage>
)

@Serializable
data class TubeMessage(
    val designation: String,
    val status: TubeStatus
)

@Serializable
sealed class TubeStatus {

    @Serializable
    object Empty : TubeStatus()

    @Serializable
    data class Reloading(val progress: Double = 0.0) : TubeStatus() {
        fun update(change: Double) = copy(progress = progress + change)
    }

    @Serializable
    object Ready : TubeStatus()

    @Serializable
    object Launching : TubeStatus()
}

@Serializable
data class ContactShieldMessage(
    val radius: Double = 1.0,
    val activated: Boolean = false
)

@Serializable
data class ShieldMessage(
    val radius: Double,
    val up: Boolean,
    val activated: Boolean,
    val strength: Double,
    val max: Double,
    val modulation: Int
) {

    fun toContactShieldMessage() =
        ContactShieldMessage(
            radius = radius,
            activated = activated
        )
}

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

@Serializable
enum class PoweredSystemType(
    private val labelOverride: String? = null
) {
    Sensors,
    Maneuver,
    Impulse("Impulse Engines"),
    Jump("Jump Drive"),
    Shields,
    Weapons,
    Reactor;

    val label: String
        get() = labelOverride ?: name

    companion object {
        fun random(): PoweredSystemType = values()[Random.nextInt(values().size)]
    }
}

@Serializable
data class PowerMessage(
    val capacitors: Double,
    val maxCapacitors: Double,
    val capacitorsPrediction: Int?,
    val settings: Map<PoweredSystemType, PoweredSystemMessage>,
    val repairProgress: RepairProgressMessage?
)

@Serializable
data class PoweredSystemMessage(
    val damage: Double,
    val level: Int,
    val heat: Double,
    val coolant: Double
)

@Serializable
data class RepairProgressMessage(
    val type: PoweredSystemType,
    val width: Int,
    val height: Int,
    val start: Int,
    val end: Int,
    val tiles: String,
    val solved: Boolean
)

@Serializable
data class ScanProgressMessage(
    val targetId: ObjectId,
    val designation: String,
    val noise: Double,
    val input: List<Double>
)

@Serializable
sealed class MapAreaMessage {

    @Serializable
    data class Polygon(
        val points: List<Vector2>
    ) : MapAreaMessage()

    @Serializable
    data class Circle(
        val center: Vector2,
        val radius: Double
    ) : MapAreaMessage()
}
