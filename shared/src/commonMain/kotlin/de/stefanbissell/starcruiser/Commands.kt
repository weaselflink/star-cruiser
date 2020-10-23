package de.stefanbissell.starcruiser

import kotlinx.serialization.Serializable

@Serializable
sealed class Command {

    @Serializable
    data class UpdateAcknowledge(val counter: Long) : Command()

    @Serializable
    object CommandTogglePause : Command()

    @Serializable
    object CommandSpawnShip : Command()

    @Serializable
    data class CommandJoinShip(val objectId: ObjectId, val station: Station) : Command()

    @Serializable
    data class CommandChangeStation(val station: Station) : Command()

    @Serializable
    object CommandExitShip : Command()

    @Serializable
    data class CommandChangeThrottle(val value: Int) : Command()

    @Serializable
    data class CommandChangeJumpDistance(val value: Double) : Command()

    @Serializable
    object CommandStartJump : Command()

    @Serializable
    data class CommandChangeRudder(val value: Int) : Command()

    @Serializable
    object CommandMapClearSelection : Command()

    @Serializable
    data class CommandMapSelectWaypoint(val index: Int) : Command()

    @Serializable
    data class CommandMapSelectShip(val targetId: ObjectId) : Command()

    @Serializable
    data class CommandAddWaypoint(val position: Vector2) : Command()

    @Serializable
    object CommandDeleteSelectedWaypoint : Command()

    @Serializable
    object CommandScanSelectedShip : Command()

    @Serializable
    object CommandAbortScan : Command()

    @Serializable
    data class CommandSolveScanGame(val dimension: Int, val value: Double) : Command()

    @Serializable
    data class CommandLockTarget(val targetId: ObjectId) : Command()

    @Serializable
    object CommandToggleShieldsUp : Command()

    @Serializable
    data class CommandStartRepair(val systemType: PoweredSystemType) : Command()

    @Serializable
    object CommandAbortRepair : Command()

    @Serializable
    data class CommandSolveRepairGame(val column: Int, val row: Int) : Command()

    @Serializable
    data class CommandSetPower(val systemType: PoweredSystemType, val power: Int) : Command()

    @Serializable
    data class CommandSetCoolant(val systemType: PoweredSystemType, val coolant: Double) : Command()

    @Serializable
    data class CommandMainScreenView(val mainScreenView: MainScreenView) : Command()

    @Serializable
    object CommandDecreaseShieldModulation : Command()

    @Serializable
    object CommandIncreaseShieldModulation : Command()

    @Serializable
    object CommandDecreaseBeamModulation : Command()

    @Serializable
    object CommandIncreaseBeamModulation : Command()

    fun toJson() = configuredJson.encodeToString(serializer(), this)

    companion object {
        fun parse(input: String): Command = configuredJson.decodeFromString(serializer(), input)
    }
}

@Serializable
enum class Station {
    Helm,
    Weapons,
    Navigation,
    Engineering,
    MainScreen
}
