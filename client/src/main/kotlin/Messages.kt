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
    val id: UUID,
    val name: String
)

@Serializable
data class ShipMessage(
    val id: UUID,
    val name: String,
    val position: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val throttle: BigDecimal,
    val thrust: BigDecimal,
    val rudder: BigDecimal,
    val history: Array<Pair<BigDecimal, Vector2>>
)

@Serializable
data class ContactMessage(
    val position: Vector2,
    val relativePosition: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val history: Array<Pair<BigDecimal, Vector2>>
)

@Serializable
data class Vector2(
    val x: BigDecimal,
    val y: BigDecimal
)

typealias UUID = String
typealias BigDecimal = String
