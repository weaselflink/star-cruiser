import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val jsonConfiguration = JsonConfiguration.Stable.copy(
    encodeDefaults = true,
    isLenient = true,
    serializeSpecialFloatingPointValues = true,
    allowStructuredMapKeys = true,
    unquotedPrint = false,
    prettyPrint = true,
    useArrayPolymorphism = true
)

@Serializable
data class GameStateMessage(
    val counter: Long,
    val snapshot: GameStateSnapshot
) {

    companion object {
        @ImplicitReflectionSerializer
        fun parse(input: String): GameStateMessage = Json(jsonConfiguration).parse(serializer(), input)
    }
}

@Serializable
data class GameStateSnapshot(
    val paused: Boolean,
    val playerShips: Array<PlayerShipMessage>,
    val ship: ShipMessage?,
    val contacts: Array<ContactMessage>
)

@Serializable
data class PlayerShipMessage(
    val id: String,
    val name: String
)

@Serializable
data class ShipMessage(
    val id: String,
    val name: String,
    val position: Vector2,
    val speed: Vector2,
    val rotation: Double,
    val heading: Double,
    val velocity: Double,
    val throttle: Double,
    val thrust: Double,
    val rudder: Double,
    val history: Array<Pair<Double, Vector2>>
)

@Serializable
data class ContactMessage(
    val position: Vector2,
    val relativePosition: Vector2,
    val speed: Vector2,
    val rotation: Double,
    val heading: Double,
    val velocity: Double,
    val history: Array<Pair<Double, Vector2>>
)

@Serializable
data class Vector2(
    val x: Double,
    val y: Double
)
