@file:UseSerializers(BigDecimalSerializer::class, UUIDSerializer::class)

package de.bissell.starcruiser

import de.bissell.starcruiser.ThrottleMessage.AcknowledgeInflightMessage
import de.bissell.starcruiser.ThrottleMessage.AddInflightMessage
import de.bissell.starcruiser.ThrottleMessage.GetInflightMessageCount
import de.bissell.starcruiser.serializers.BigDecimalSerializer
import de.bissell.starcruiser.serializers.UUIDSerializer
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.util.UUID

@ObsoleteCoroutinesApi
class GameClient(
    private val id: UUID = UUID.randomUUID(),
    private val gameStateActor: SendChannel<GameStateChange>,
    private val outgoing: SendChannel<Frame>,
    private val incoming: ReceiveChannel<Frame>
) {

    private val maxInflightMessages: Int = 3
    private val updateIntervalMillis: Long = 10

    suspend fun start(coroutineScope: CoroutineScope) {
        val throttleActor = coroutineScope.createThrottleActor()
        gameStateActor.send(NewGameClient(id))

        val updateJob = coroutineScope.launch {
            while (isActive) {
                if (throttleActor.getInflightMessageCount() < maxInflightMessages) {
                    val counterResponse = throttleActor.addInflightMessage()
                    val gameStateSnapShot = gameStateActor.getGameStateSnapShot()
                    outgoing.sendText(
                        GameStateMessage(
                            counterResponse,
                            gameStateSnapShot
                        ).toJson()
                    )
                }
                delay(updateIntervalMillis)
            }
        }

        for (frame in incoming) {
            val input = String(frame.data)
            when (val command = Command.parse(input)) {
                is Command.UpdateAcknowledge -> throttleActor.send(AcknowledgeInflightMessage(command.counter))
                is Command.CommandTogglePause -> gameStateActor.send(TogglePause)
                is Command.CommandSpawnShip -> gameStateActor.send(TogglePause)
                is Command.CommandJoinShip -> gameStateActor.send(JoinShip(id, command.shipId))
                is Command.CommandChangeThrottle -> gameStateActor.send(ChangeThrottle(id, BigDecimal(command.diff)))
                is Command.CommandChangeRudder -> gameStateActor.send(ChangeRudder(id, BigDecimal(command.diff)))
            }
        }

        updateJob.cancelAndJoin()
        gameStateActor.send(GameClientDisconnected(id))
    }

    private suspend fun SendChannel<ThrottleMessage>.getInflightMessageCount(): Int {
        val response = CompletableDeferred<Int>()
        send(GetInflightMessageCount(response))
        return response.await()
    }

    private suspend fun SendChannel<ThrottleMessage>.addInflightMessage(): Long {
        val response = CompletableDeferred<Long>()
        send(AddInflightMessage(response))
        return response.await()
    }

    private suspend fun SendChannel<GameStateChange>.getGameStateSnapShot(): GameStateSnapshot {
        val response = CompletableDeferred<GameStateSnapshot>()
        send(GetGameStateSnapshot(id, response))
        return response.await()
    }

    companion object {
        suspend fun CoroutineScope.startGameClient(
            gameStateActor: SendChannel<GameStateChange>,
            outgoing: SendChannel<Frame>,
            incoming: ReceiveChannel<Frame>
        ) =
            GameClient(
                gameStateActor = gameStateActor,
                outgoing = outgoing,
                incoming = incoming
            ).start(this)
    }
}

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
    val position: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val throttle: BigDecimal,
    val thrust: BigDecimal,
    val rudder: BigDecimal,
    val history: List<Pair<BigDecimal, Vector2>>
)

@Serializable
data class ContactMessage(
    val position: Vector2,
    val relativePosition: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val history: List<Pair<BigDecimal, Vector2>>
)
