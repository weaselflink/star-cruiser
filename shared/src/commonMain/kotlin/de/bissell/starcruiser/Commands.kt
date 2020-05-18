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
    class CommandJoinShip(val shipId: String, val station: Station) : Command()

    @Serializable
    class CommandChangeStation(val station: Station) : Command()

    @Serializable
    object CommandExitShip : Command()

    @Serializable
    class CommandChangeThrottle(val value: Int) : Command()

    @Serializable
    class CommandChangeRudder(val value: Int) : Command()

    fun toJson() = Json(jsonConfiguration).stringify(serializer(), this)

    companion object {
        fun parse(input: String): Command = Json(jsonConfiguration).parse(serializer(), input)
    }
}

@Serializable
enum class Station {
    Helm,
    Navigation
}
