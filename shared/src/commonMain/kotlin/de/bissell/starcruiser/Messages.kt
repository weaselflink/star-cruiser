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
    val snapshot: GameStateSnapshot
) {
    fun toJson(): String = Json(jsonConfiguration).stringify(serializer(), this)

    companion object {
        fun parse(input: String): GameStateMessage = Json(jsonConfiguration).parse(serializer(), input)
    }
}

@Serializable
data class GameStateSnapshot(
    val clientState: ClientState,
    val paused: Boolean,
    val playerShips: List<PlayerShipMessage>,
    val ship: ShipMessage?,
    val contacts: List<ContactMessage>
)

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
    val shortRangeScopeRange: Double
)

@Serializable
data class ContactMessage(
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
enum class ClientState {
    ShipSelection,
    Helm,
    Navigation
}
