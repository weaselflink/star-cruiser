@file:UseSerializers(BigDecimalSerializer::class, UUIDSerializer::class)

package de.bissell.starcruiser

import de.bissell.starcruiser.serializers.BigDecimalSerializer
import de.bissell.starcruiser.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
sealed class Command {

    @Serializable
    class UpdateAcknowledge(val counter: Long) : Command()

    @Serializable
    object CommandTogglePause : Command()

    @Serializable
    object CommandSpawnShip: Command()

    @Serializable
    class CommandJoinShip(val shipId: UUID) : Command()

    @Serializable
    class CommandChangeThrottle(val diff: Long) : Command()

    @Serializable
    class CommandChangeRudder(val diff: Long) : Command()

    companion object {
        fun parse(input: String): Command = Json(jsonConfiguration).parse(serializer(), input)
    }
}
