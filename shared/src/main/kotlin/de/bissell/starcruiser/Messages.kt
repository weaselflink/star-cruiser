@file:UseSerializers(BigDecimalSerializer::class, UUIDSerializer::class)

package de.bissell.starcruiser

import de.bissell.starcruiser.serializers.BigDecimalSerializer
import de.bissell.starcruiser.serializers.UUIDSerializer
import io.ktor.serialization.DefaultJsonConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.util.*

val jsonConfiguration = DefaultJsonConfiguration.copy(
    prettyPrint = true,
    useArrayPolymorphism = false
)

@Serializable
data class GameStateMessage(
    val counter: Long,
    val snapshot: GameStateSnapshot
) {
    fun toJson(): String = Json(jsonConfiguration).stringify(serializer(), this)
}

@Serializable
data class GameStateSnapshot(
    val paused: Boolean,
    val playerShips: List<PlayerShipMessage>,
    val ship: ShipMessage?,
    val contacts: List<ContactMessage>
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
    val position: BigVector,
    val speed: BigVector,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val throttle: BigDecimal,
    val thrust: BigDecimal,
    val rudder: BigDecimal,
    val history: List<Pair<BigDecimal, BigVector>>
)

@Serializable
data class ContactMessage(
    val position: BigVector,
    val relativePosition: BigVector,
    val speed: BigVector,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val history: List<Pair<BigDecimal, BigVector>>
)
