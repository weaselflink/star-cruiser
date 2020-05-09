
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UpdateAcknowledge(
    val type: String = "de.bissell.starcruiser.Command.UpdateAcknowledge",
    val counter: Int
) {
    fun toJson() = Json(jsonConfiguration).stringify(serializer(), this)
}

@Serializable
object CommandTogglePause {
    const val type: String = "de.bissell.starcruiser.Command.CommandTogglePause"

    fun toJson() = Json(jsonConfiguration).stringify(serializer(), this)
}

@Serializable
data class CommandJoinShip(
    val type: String = "de.bissell.starcruiser.Command.CommandJoinShip",
    val shipId: String
) {
    fun toJson() = Json(jsonConfiguration).stringify(serializer(), this)
}

@Serializable
data class CommandChangeThrottle(
    val type: String = "de.bissell.starcruiser.Command.CommandChangeThrottle",
    val diff: Int
) {
    fun toJson() = Json(jsonConfiguration).stringify(serializer(), this)
}


@Serializable
data class CommandChangeRudder(
    val type: String = "de.bissell.starcruiser.Command.CommandChangeRudder",
    val diff: Int
) {
    fun toJson() = Json(jsonConfiguration).stringify(serializer(), this)
}
