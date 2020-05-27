package de.bissell.starcruiser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val jsonConfiguration = JsonConfiguration.Stable.copy(
    encodeDefaults = true,
    isLenient = true,
    prettyPrint = true
)

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

    @Serializable
    data class ShipSelection(
        val playerShips: List<PlayerShipMessage>
    ) : SnapshotMessage()

    @Serializable
    data class Helm(
        override val ship: ShipMessage,
        val contacts: List<ContactMessage>
    ) : SnapshotMessage(), ShipSnapshot

    @Serializable
    data class Navigation(
        override val ship: ShipMessage
    ) : SnapshotMessage(), ShipSnapshot

    @Serializable
    data class MainScreen(
        override val ship: ShipMessage,
        val contacts: List<ContactMessage>
    ) : SnapshotMessage(), ShipSnapshot
}

@Serializable
data class PlayerShipMessage(
    val id: String,
    val name: String,
    val shipClass: String?
)

@Serializable
data class ShipMessage(
    val id: String,
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
    val waypoints: List<WaypointMessage>
)

@Serializable
data class ContactMessage(
    val id: String,
    val designation: String,
    val position: Vector2,
    val relativePosition: Vector2,
    val speed: Vector2,
    val rotation: Double,
    val heading: Double,
    val velocity: Double,
    val history: List<Pair<Double, Vector2>>
)

@Serializable
data class WaypointMessage(
    val name: String,
    val position: Vector2,
    val relativePosition: Vector2
)
