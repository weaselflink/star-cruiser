package de.bissell.starcruiser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class Command {

    @Serializable
    data class UpdateAcknowledge(val counter: Long) : Command()

    @Serializable
    object CommandTogglePause : Command()

    @Serializable
    object CommandSpawnShip: Command()

    @Serializable
    data class CommandJoinShip(val shipId: String, val station: Station) : Command()

    @Serializable
    data class CommandChangeStation(val station: Station) : Command()

    @Serializable
    object CommandExitShip : Command()

    @Serializable
    data class CommandChangeThrottle(val value: Int) : Command()

    @Serializable
    data class CommandChangeRudder(val value: Int) : Command()

    @Serializable
    data class CommandAddWaypoint(val position: Vector2) : Command()

    @Serializable
    data class CommandDeleteWaypoint(val position: Vector2) : Command()

    fun toJson() = Json(jsonConfiguration).stringify(serializer(), this)

    companion object {
        fun parse(input: String): Command = Json(jsonConfiguration).parse(serializer(), input)
    }
}

@Serializable
enum class Station {
    Helm,
    Navigation,
    MainScreen
}
