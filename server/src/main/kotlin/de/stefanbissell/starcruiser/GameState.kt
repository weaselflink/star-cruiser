package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ClientState.InShip
import de.stefanbissell.starcruiser.ClientState.ShipDestroyed
import de.stefanbissell.starcruiser.ClientState.ShipSelection
import de.stefanbissell.starcruiser.Station.Engineering
import de.stefanbissell.starcruiser.Station.Helm
import de.stefanbissell.starcruiser.Station.MainScreen
import de.stefanbissell.starcruiser.Station.Navigation
import de.stefanbissell.starcruiser.Station.Weapons
import de.stefanbissell.starcruiser.client.ClientId
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.PlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.random.Random

sealed class GameStateChange

object Update : GameStateChange()
data class GetGameStateSnapshot(val clientId: ClientId, val response: CompletableDeferred<SnapshotMessage>) : GameStateChange()
object TogglePause : GameStateChange()
object SpawnShip : GameStateChange()
data class JoinShip(val clientId: ClientId, val objectId: ObjectId, val station: Station) : GameStateChange()
data class ChangeStation(val clientId: ClientId, val station: Station) : GameStateChange()
data class ExitShip(val clientId: ClientId) : GameStateChange()
data class NewGameClient(val clientId: ClientId) : GameStateChange()
data class GameClientDisconnected(val clientId: ClientId) : GameStateChange()
data class SetThrottle(val clientId: ClientId, val value: Int) : GameStateChange()
data class ChangeJumpDistance(val clientId: ClientId, val value: Double) : GameStateChange()
data class StartJump(val clientId: ClientId) : GameStateChange()
data class SetRudder(val clientId: ClientId, val value: Int) : GameStateChange()
data class MapClearSelection(val clientId: ClientId) : GameStateChange()
data class MapSelectWaypoint(val clientId: ClientId, val index: Int) : GameStateChange()
data class MapSelectShip(val clientId: ClientId, val targetId: ObjectId) : GameStateChange()
data class AddWaypoint(val clientId: ClientId, val position: Vector2) : GameStateChange()
data class DeleteSelectedWaypoint(val clientId: ClientId) : GameStateChange()
data class ScanSelectedShip(val clientId: ClientId) : GameStateChange()
data class AbortScan(val clientId: ClientId) : GameStateChange()
data class SolveScanGame(val clientId: ClientId, val dimension: Int, val value: Double) : GameStateChange()
data class LockTarget(val clientId: ClientId, val targetId: ObjectId) : GameStateChange()
data class ToggleShieldsUp(val clientId: ClientId) : GameStateChange()
data class StartRepair(val clientId: ClientId, val systemType: PoweredSystemType) : GameStateChange()
data class AbortRepair(val clientId: ClientId) : GameStateChange()
data class SolveRepairGame(val clientId: ClientId, val column: Int, val row: Int) : GameStateChange()
data class SetPower(val clientId: ClientId, val systemType: PoweredSystemType, val power: Int) : GameStateChange()
data class SetCoolant(val clientId: ClientId, val systemType: PoweredSystemType, val coolant: Double) : GameStateChange()
data class SetMainScreenView(val clientId: ClientId, val mainScreenView: MainScreenView) : GameStateChange()

class GameState {

    private var time = GameTime()
    private val ships = mutableMapOf<ObjectId, Ship>()
    private val playerShips
        get() = ships.values
            .filterIsInstance<PlayerShip>()
            .associateBy { it.id }
    private val asteroids = mutableListOf<Asteroid>()
    private val clients = mutableMapOf<ClientId, Client>()

    private val physicsEngine = PhysicsEngine()

    init {
        repeat(50) {
            spawnAsteroid()
        }
        repeat(4) {
            spawnNonPlayerShip()
        }
    }

    fun toMessage(clientId: ClientId): SnapshotMessage {
        val client = getClient(clientId)
        return when (val state = client.state) {
            is ShipSelection -> SnapshotMessage.ShipSelection(
                playerShips = playerShips.values
                    .map(PlayerShip::toPlayerShipMessage)
            )
            is ShipDestroyed -> SnapshotMessage.ShipDestroyed
            is InShip -> {
                val ship = state.ship
                when (state.station) {
                    Helm -> SnapshotMessage.Helm(
                        shortRangeScope = ship.toShortRangeScopeMessage(),
                        contacts = getScopeContacts(ship),
                        asteroids = getScopeAsteroids(ship),
                        throttle = ship.throttle,
                        rudder = ship.rudder,
                        jumpDrive = ship.toJumpDriveMessage()
                    )
                    Weapons -> SnapshotMessage.Weapons(
                        shortRangeScope = ship.toShortRangeScopeMessage(),
                        contacts = getScopeContacts(ship),
                        asteroids = getScopeAsteroids(ship),
                        hull = ship.hull,
                        hullMax = ship.template.hull,
                        shield = ship.toShieldMessage()
                    )
                    Navigation -> SnapshotMessage.Navigation(
                        ship = ship.toNavigationMessage { id -> ships[id] },
                        mapSelection = ship.toMapSelectionMessage { id -> ships[id] },
                        contacts = getMapContacts(ship),
                        asteroids = getMapAsteroids(ship)
                    )
                    Engineering -> SnapshotMessage.Engineering(
                        powerSettings = ship.toPowerMessage()
                    )
                    MainScreen -> toMainScreenMessage(ship)
                }
            }
        }
    }

    private fun toMainScreenMessage(ship: PlayerShip): SnapshotMessage =
        when (ship.mainScreenView) {
            MainScreenView.Scope -> toMainScreenShortRangeScope(ship)
            else -> toMainScreen3d(ship)
        }

    private fun toMainScreenShortRangeScope(ship: PlayerShip) =
        SnapshotMessage.MainScreenShortRangeScope(
            shortRangeScope = ship.toShortRangeScopeMessage(),
            contacts = getScopeContacts(ship),
            asteroids = getScopeAsteroids(ship)
        )

    private fun toMainScreen3d(ship: PlayerShip) =
        SnapshotMessage.MainScreen3d(
            ship = ship.toMessage(),
            contacts = getContacts(ship),
            asteroids = getAsteroids(ship)
        )

    fun clientConnected(clientId: ClientId) {
        getClient(clientId)
    }

    fun clientDisconnected(clientId: ClientId) {
        clients.remove(clientId)
    }

    fun joinShip(clientId: ClientId, objectId: ObjectId, station: Station) {
        playerShips[objectId]?.also {
            getClient(clientId).joinShip(it, station)
        }
    }

    fun changeStation(clientId: ClientId, station: Station) {
        getClient(clientId).changeStation(station)
    }

    fun exitShip(clientId: ClientId) {
        getClient(clientId).exitShip()
    }

    fun spawnShip() {
        PlayerShip(
            position = Vector2.random(300),
            rotation = Random.nextDouble(PI * 2.0)
        ).also {
            it.addWaypoint(Vector2.random(1000, 500))
            it.addWaypoint(Vector2.random(1000, 500))
            ships[it.id] = it
            physicsEngine.addShip(it)
            it.toggleShieldsUp()
            it.takeDamage(PoweredSystemType.Jump, 2.5)
            it.toggleShieldsUp()
        }
    }

    fun spawnNonPlayerShip() {
        NonPlayerShip(
            position = Vector2.random(1050, 850),
            rotation = Random.nextDouble(PI * 2.0)
        ).also {
            it.throttle = 50
            ships[it.id] = it
            physicsEngine.addShip(it)
        }
    }

    private fun spawnAsteroid() {
        Asteroid(
            position = Vector2.random(800, 200),
            rotation = Random.nextDouble(PI * 2.0),
            radius = Random.nextDouble(8.0, 32.0)
        ).also {
            asteroids += it
            physicsEngine.addAsteroid(it)
        }
    }

    fun togglePaused() {
        time.paused = !time.paused
    }

    fun update() {
        if (time.paused) return

        time.update(now())

        physicsEngine.step(time.delta)
        updateShips()
        updateAsteroids()
    }

    private fun updateShips() {
        ships.forEach { shipEntry ->
            shipEntry.value.apply {
                val contactList = ships.values.filter { it.id != shipEntry.key }
                update(time, physicsEngine, contactList) { id -> ships[id] }
            }
        }
        ships.map {
            it.value.endUpdate(physicsEngine)
        }.filter {
            it.destroyed
        }.forEach {
            destroyShip(it.id)
        }
    }

    private fun updateAsteroids() {
        asteroids.forEach {
            it.update(physicsEngine)
        }
    }

    fun setThrottle(clientId: ClientId, value: Int) {
        getClientShip(clientId)?.setThrottle(value)
    }

    fun changeJumpDistance(clientId: ClientId, value: Double) {
        getClientShip(clientId)?.changeJumpDistance(value)
    }

    fun startJump(clientId: ClientId) {
        getClientShip(clientId)?.startJump()
    }

    fun setRudder(clientId: ClientId, value: Int) {
        getClientShip(clientId)?.rudder = value
    }

    fun mapClearSelection(clientId: ClientId) {
        getClientShip(clientId)?.mapClearSelection()
    }

    fun mapSelectWaypoint(clientId: ClientId, index: Int) {
        getClientShip(clientId)?.mapSelectWaypoint(index)
    }

    fun mapSelectShip(clientId: ClientId, targetId: ObjectId) {
        getClientShip(clientId)?.mapSelectShip(targetId)
    }

    fun addWaypoint(clientId: ClientId, position: Vector2) {
        getClientShip(clientId)?.addWaypoint(position)
    }

    fun deleteSelectedWaypoint(clientId: ClientId) {
        getClientShip(clientId)?.deleteSelectedWaypoint()
    }

    fun scanSelectedShip(clientId: ClientId) {
        getClientShip(clientId)?.startScan()
    }

    fun abortScan(clientId: ClientId) {
        getClientShip(clientId)?.abortScan()
    }

    fun solveScanGame(clientId: ClientId, dimension: Int, value: Double) {
        getClientShip(clientId)?.solveScanGame(dimension, value)
    }

    fun lockTarget(clientId: ClientId, targetId: ObjectId) {
        ships[targetId]?.also {
            getClientShip(clientId)?.lockTarget(targetId)
        }
    }

    fun setShieldsUp(clientId: ClientId, value: Boolean) {
        getClientShip(clientId)?.setShieldsUp(value)
    }

    fun toggleShieldsUp(clientId: ClientId) {
        getClientShip(clientId)?.toggleShieldsUp()
    }

    fun startRepair(clientId: ClientId, systemType: PoweredSystemType) {
        getClientShip(clientId)?.startRepair(systemType)
    }

    fun abortRepair(clientId: ClientId) {
        getClientShip(clientId)?.abortRepair()
    }

    fun solveRepairGame(clientId: ClientId, column: Int, row: Int) {
        getClientShip(clientId)?.solveRepairGame(column, row)
    }

    fun setPower(clientId: ClientId, systemType: PoweredSystemType, power: Int) {
        getClientShip(clientId)?.setPower(systemType, power)
    }

    fun setCoolant(clientId: ClientId, systemType: PoweredSystemType, coolant: Double) {
        getClientShip(clientId)?.setCoolant(systemType, coolant)
    }

    fun setMainScreenView(clientId: ClientId, mainScreenView: MainScreenView) {
        getClientShip(clientId)?.also {
            it.mainScreenView = mainScreenView
        }
    }

    private fun getClient(clientId: ClientId) =
        clients.computeIfAbsent(clientId) { Client(clientId) }

    private fun getClientShip(clientId: ClientId): PlayerShip? =
        getClient(clientId).state.let {
            if (it is InShip) it.ship else null
        }

    private fun getContacts(clientShip: PlayerShip): List<ContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toContactMessage(clientShip) }
    }

    private fun getMapContacts(clientShip: PlayerShip): List<MapContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .filter { clientShip.inSensorRange(it.value) }
            .map { it.value }
            .map { it.toMapContactMessage(clientShip) }
    }

    private fun getMapAsteroids(clientShip: PlayerShip): List<AsteroidMessage> {
        return asteroids
            .filter { clientShip.inSensorRange(it) }
            .map { it.toMessage(clientShip) }
    }

    private fun getScopeContacts(clientShip: PlayerShip): List<ScopeContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toScopeContactMessage(clientShip) }
            .filter {
                it.relativePosition.length() < clientShip.template.shortRangeScopeRange * 1.1
            }
    }

    private fun getScopeAsteroids(clientShip: PlayerShip): List<AsteroidMessage> {
        return asteroids
            .map { it.toMessage(clientShip) }
            .filter {
                it.relativePosition.length() < clientShip.template.shortRangeScopeRange * 1.1
            }
    }

    private fun getAsteroids(clientShip: PlayerShip): List<AsteroidMessage> {
        return asteroids
            .map { it.toMessage(clientShip) }
    }

    private fun destroyShip(shipId: ObjectId) {
        clients.values.filter { client ->
            client.state.let { it is InShip && it.ship.id == shipId }
        }.forEach {
            it.shipDestroyed()
        }
        ships.values.forEach {
            it.targetDestroyed(shipId)
        }
        ships.remove(shipId)
        physicsEngine.removeObject(shipId)
    }

    companion object {
        fun CoroutineScope.gameStateActor() = actor<GameStateChange> {
            val gameState = GameState()
            for (change in channel) {
                when (change) {
                    is Update -> gameState.update()
                    is TogglePause -> gameState.togglePaused()
                    is GetGameStateSnapshot -> change.response.complete(gameState.toMessage(change.clientId))
                    is NewGameClient -> gameState.clientConnected(change.clientId)
                    is GameClientDisconnected -> gameState.clientDisconnected(change.clientId)
                    is JoinShip -> gameState.joinShip(change.clientId, change.objectId, change.station)
                    is ChangeStation -> gameState.changeStation(change.clientId, change.station)
                    is ExitShip -> gameState.exitShip(change.clientId)
                    is SpawnShip -> gameState.spawnShip()
                    is SetThrottle -> gameState.setThrottle(change.clientId, change.value)
                    is ChangeJumpDistance -> gameState.changeJumpDistance(change.clientId, change.value)
                    is StartJump -> gameState.startJump(change.clientId)
                    is SetRudder -> gameState.setRudder(change.clientId, change.value)
                    is MapClearSelection -> gameState.mapClearSelection(change.clientId)
                    is MapSelectWaypoint -> gameState.mapSelectWaypoint(change.clientId, change.index)
                    is MapSelectShip -> gameState.mapSelectShip(change.clientId, change.targetId)
                    is AddWaypoint -> gameState.addWaypoint(change.clientId, change.position)
                    is DeleteSelectedWaypoint -> gameState.deleteSelectedWaypoint(change.clientId)
                    is ScanSelectedShip -> gameState.scanSelectedShip(change.clientId)
                    is AbortScan -> gameState.abortScan(change.clientId)
                    is SolveScanGame -> gameState.solveScanGame(change.clientId, change.dimension, change.value)
                    is LockTarget -> gameState.lockTarget(change.clientId, change.targetId)
                    is ToggleShieldsUp -> gameState.toggleShieldsUp(change.clientId)
                    is StartRepair -> gameState.startRepair(change.clientId, change.systemType)
                    is AbortRepair -> gameState.abortRepair(change.clientId)
                    is SolveRepairGame -> gameState.solveRepairGame(change.clientId, change.column, change.row)
                    is SetPower -> gameState.setPower(change.clientId, change.systemType, change.power)
                    is SetCoolant -> gameState.setCoolant(change.clientId, change.systemType, change.coolant)
                    is SetMainScreenView -> gameState.setMainScreenView(change.clientId, change.mainScreenView)
                }
            }
        }
    }
}

class GameTime(
    initialTime: Instant? = null
) {

    private var lastUpdate: Instant? = initialTime

    var current: Double = 0.0
        private set

    var delta: Double = 0.001
        private set

    var paused: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                lastUpdate = null
            }
        }

    fun update(now: Instant) {
        delta = lastUpdate?.let {
            (it.until(now, ChronoUnit.MILLIS)) / 1_000.0
        } ?: 0.001
        current += delta
        lastUpdate = now
    }

    fun update(seconds: Double) {
        delta = seconds
        current += delta
        lastUpdate = lastUpdate?.plusMillis((seconds * 1_000).roundToLong())
    }
}

data class Client(
    val id: ClientId
) {

    var state: ClientState = ShipSelection
        private set

    fun joinShip(ship: PlayerShip, station: Station) {
        state = InShip(
            ship = ship,
            station = station
        )
    }

    fun changeStation(station: Station) {
        val currentState = state
        if (currentState is InShip) {
            state = InShip(
                ship = currentState.ship,
                station = station
            )
        }
    }

    fun shipDestroyed() {
        state = ShipDestroyed
    }

    fun exitShip() {
        state = ShipSelection
    }
}

sealed class ClientState {

    object ShipSelection : ClientState()

    object ShipDestroyed : ClientState()

    data class InShip(
        val ship: PlayerShip,
        val station: Station
    ) : ClientState()
}
