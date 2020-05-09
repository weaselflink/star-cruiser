sealed class Command {

    class UpdateAcknowledge(val counter: Long) : Command()

    object CommandTogglePause : Command()

    object CommandSpawnShip: Command()

    class CommandJoinShip(val shipId: String) : Command()

    class CommandChangeThrottle(val diff: Long) : Command()

    class CommandChangeRudder(val diff: Long) : Command()
}
