package de.bissell.starcruiser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class Command {

    @Serializable
    class UpdateAcknowledge(val counter: Long) : Command()

    @Serializable
    object CommandTogglePause : Command()

    @Serializable
    object CommandSpawnShip: Command()

    @Serializable
    class CommandJoinShip(val shipId: String) : Command()

    @Serializable
    class CommandChangeThrottle(val diff: Long) : Command()

    @Serializable
    class CommandChangeRudder(val diff: Long) : Command()

    companion object {
        fun parse(input: String): Command = Json(jsonConfiguration).parse(serializer(), input)
    }
}
