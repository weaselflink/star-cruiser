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
import de.stefanbissell.starcruiser.scenario.GameStateMutator
import de.stefanbissell.starcruiser.scenario.GameStateView
import de.stefanbissell.starcruiser.scenario.TestScenario
import de.stefanbissell.starcruiser.scenario.TriggerHandler
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.PlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.combat.DamageEvent
import de.stefanbissell.starcruiser.ships.combat.Torpedo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.math.PI
import kotlin.random.Random

class GameState {

    private var time = GameTime()
    private val scenario = TestScenario.create()
    private val triggerHandlers = scenario.triggers.map {
        TriggerHandler(it)
    }
    private val ships = mutableMapOf<ObjectId, Ship>()
    private val torpedoes = mutableMapOf<ObjectId, Torpedo>()
    private val playerShips
        get() = ships.values
            .filterIsInstance<PlayerShip>()
            .associateBy { it.id }
    private val asteroids = mutableListOf<Asteroid>()
    private val clients = mutableMapOf<ClientId, Client>()
    private val physicsEngine = PhysicsEngine()
    private val unhandledEvents = mutableListOf<DamageEvent>()

    init {
        scenario.asteroids.forEach {
            spawnAsteroid(it)
        }
        scenario.nonPlayerShips.forEach {
            spawnNonPlayerShip(it)
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
                val contactList = ContactList(ship, ships, torpedoes)
                when (state.station) {
                    Helm -> toHelmMessage(ship, contactList)
                    Weapons -> toWeaponsMessage(ship, contactList)
                    Navigation -> toNavigationMessage(ship, contactList)
                    Engineering -> toEngineeringMessage(ship)
                    MainScreen -> toMainScreenMessage(ship, contactList)
                }
            }
        }
    }

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

    fun spawnPlayerShip() {
        scenario.playerSpawnArea.randomPointInside().let {
            spawnPlayerShip(it)
        }
    }

    fun spawnNonPlayerShip(ship: NonPlayerShip) {
        ship.also {
            ships[it.id] = it
            physicsEngine.addShip(it)
        }
    }

    fun togglePaused() {
        time.paused = !time.paused
    }

    fun update() {
        if (time.paused) return

        time.update(Clock.System.now())

        unhandledEvents.clear()
        physicsEngine.step(time.delta)
        updateAsteroids()
        updateTorpedoes()
        updateShips()
        handleCollisions(physicsEngine.collisions)
        handleEvents()
        removeDestroyed()
        updateTriggers()
    }

    fun setThrottle(clientId: ClientId, value: Int) {
        getClientShip(clientId)?.throttle = value
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

    fun decreaseShieldModulation(clientId: ClientId) {
        getClientShip(clientId)?.decreaseShieldModulation()
    }

    fun increaseShieldModulation(clientId: ClientId) {
        getClientShip(clientId)?.increaseShieldModulation()
    }

    fun decreaseBeamModulation(clientId: ClientId) {
        getClientShip(clientId)?.decreaseBeamModulation()
    }

    fun increaseBeamModulation(clientId: ClientId) {
        getClientShip(clientId)?.increaseBeamModulation()
    }

    fun reloadTube(clientId: ClientId, index: Int) {
        getClientShip(clientId)?.reloadTube(index)
    }

    fun launchTube(clientId: ClientId, index: Int) {
        getClientShip(clientId)?.launchTube(index)
    }

    fun toView() =
        GameStateView(
            currentTime = time.current,
            scenario = scenario,
            ships = ships.values.toList()
        )

    private fun toHelmMessage(
        ship: PlayerShip,
        contactList: ContactList
    ): SnapshotMessage.Helm {
        return SnapshotMessage.Helm(
            shortRangeScope = ship.toShortRangeScopeMessage(),
            contacts = contactList.getScopeContacts(),
            asteroids = getScopeAsteroids(ship),
            throttle = ship.throttle,
            rudder = ship.rudder,
            jumpDrive = ship.toJumpDriveMessage()
        )
    }

    private fun toWeaponsMessage(
        ship: PlayerShip,
        contactList: ContactList
    ): SnapshotMessage.Weapons {
        return SnapshotMessage.Weapons(
            shortRangeScope = ship.toShortRangeScopeMessage(),
            contacts = contactList.getScopeContacts(),
            asteroids = getScopeAsteroids(ship),
            hull = ship.hull,
            hullMax = ship.template.hull,
            shield = ship.toShieldMessage(),
            tubes = ship.toTubesMessage()
        )
    }

    private fun toNavigationMessage(
        ship: PlayerShip,
        contactList: ContactList
    ): SnapshotMessage.Navigation {
        return SnapshotMessage.Navigation(
            ship = ship.toNavigationMessage(contactList),
            mapSelection = ship.toMapSelectionMessage(contactList),
            contacts = contactList.getMapContacts(),
            asteroids = getMapAsteroids(ship),
            mapAreas = scenario.mapAreas.map { it.shape.toMessage() }
        )
    }

    private fun toEngineeringMessage(ship: PlayerShip): SnapshotMessage.Engineering {
        return SnapshotMessage.Engineering(
            powerSettings = ship.toPowerMessage()
        )
    }

    private fun toMainScreenMessage(ship: PlayerShip, contactList: ContactList): SnapshotMessage =
        when (ship.mainScreenView) {
            MainScreenView.Scope -> toMainScreenShortRangeScope(ship, contactList)
            else -> toMainScreen3d(ship, contactList)
        }

    private fun toMainScreenShortRangeScope(ship: PlayerShip, contactList: ContactList) =
        SnapshotMessage.MainScreenShortRangeScope(
            shortRangeScope = ship.toShortRangeScopeMessage(),
            contacts = contactList.getScopeContacts(),
            asteroids = getScopeAsteroids(ship)
        )

    private fun toMainScreen3d(ship: PlayerShip, contactList: ContactList) =
        SnapshotMessage.MainScreen3d(
            ship = ship.toMessage(),
            contacts = contactList.getContacts(),
            asteroids = getAsteroids(ship)
        )

    private fun spawnPlayerShip(position: Vector2) {
        PlayerShip(
            faction = scenario.factions.first { it.forPlayers },
            position = position,
            rotation = Random.nextDouble(PI * 2.0)
        ).also {
            it.addWaypoint(Vector2.random(1000, 500))
            it.addWaypoint(Vector2.random(1000, 500))
            ships[it.id] = it
            physicsEngine.addShip(it)
            it.toggleShieldsUp()
            it.applyDamage(DamageEvent.Beam(it.id, PoweredSystemType.Jump, 2.5, 0))
            it.toggleShieldsUp()
        }
    }

    private fun spawnAsteroid(asteroid: Asteroid) {
        asteroid.also {
            asteroids += it
            physicsEngine.addAsteroid(it)
        }
    }

    private fun updateShips() {
        val updateResults = ships.values
            .onEach { ship ->
                ship.apply {
                    val contactList = ContactList(this, ships, torpedoes)
                    update(time, physicsEngine, contactList)
                }
            }
            .map { ship ->
                ship.updateResult
            }

        updateResults.flatMap {
            it.torpedoes
        }.forEach { torpedo ->
            torpedoes[torpedo.id] = torpedo
            ships[torpedo.launcherId]?.also { launcher ->
                launcher.scans[torpedo.id] = ScanLevel.Detailed
            }

            physicsEngine.addTorpedo(torpedo)
        }

        unhandledEvents += updateResults.flatMap { it.damageEvents }

        ships.filter {
            it.value.destroyed
        }.forEach {
            destroyShip(it.value.id)
        }
    }

    private fun updateTorpedoes() {
        torpedoes.values.forEach { torpedoEntry ->
            torpedoEntry.apply {
                update(time, physicsEngine)
            }
        }
    }

    private fun updateAsteroids() {
        asteroids.forEach {
            it.update(physicsEngine)
        }
    }

    private fun handleCollisions(collisions: List<Pair<ObjectId, ObjectId>>) {
        collisions.filter {
            torpedoes.containsKey(it.first) || torpedoes.containsKey(it.second)
        }.map { pair ->
            if (torpedoes.containsKey(pair.first)) {
                torpedoes[pair.first]!! to pair.second
            } else {
                torpedoes[pair.second]!! to pair.first
            }
        }.filter { (torpedo, _) ->
            torpedo.timeSinceLaunch > 0.1
        }.forEach { (torpedo, other) ->
            val damage = torpedo.template.warhead.damage
            unhandledEvents += DamageEvent.Torpedo(other, damage)
            unhandledEvents += DamageEvent.Torpedo(torpedo.id, damage)
        }
    }

    private fun handleEvents() {
        unhandledEvents.forEach { damageEvent ->
            getDynamicObject(damageEvent.target)
                ?.also {
                    it.applyDamage(damageEvent)
                }
        }
    }

    private fun removeDestroyed() {
        ships.filter {
            it.value.destroyed
        }.forEach {
            destroyShip(it.value.id)
        }

        torpedoes.values.filter {
            it.destroyed
        }.forEach {
            destroyTorpedo(it.id)
        }
    }

    private fun getDynamicObject(id: ObjectId) = ships[id] ?: torpedoes[id]

    private fun updateTriggers() {
        val gameStateMutator = GameStateMutator(
            state = this,
            scenario = scenario
        )
        triggerHandlers.forEach {
            it.evaluate(gameStateMutator)
        }
    }

    private fun getClient(clientId: ClientId) =
        clients.computeIfAbsent(clientId) { Client(clientId) }

    private fun getClientShip(clientId: ClientId): PlayerShip? =
        getClient(clientId).state.let {
            if (it is InShip) it.ship else null
        }

    private fun ContactList.getContacts(): List<ContactMessage> {
        return contacts.values
            .mapNotNull { it.contactMessage }
    }

    private fun ContactList.getMapContacts(): List<MapContactMessage> {
        return allInSensorRange()
            .map { it.toMapContactMessage() }
    }

    private fun ContactList.getScopeContacts(): List<ScopeContactMessage> {
        return allNearScopeRange()
            .map { it.toScopeContactMessage() }
    }

    private fun getAsteroids(clientShip: PlayerShip): List<AsteroidMessage> {
        return asteroids
            .map { it.toMessage(clientShip) }
    }

    private fun getScopeAsteroids(clientShip: Ship): List<ScopeAsteroidMessage> {
        return asteroids
            .map { it.toScopeMessage(clientShip) }
            .filter {
                it.relativePosition.length() < clientShip.template.shortRangeScopeRange * 1.1
            }
    }

    private fun getMapAsteroids(clientShip: PlayerShip): List<MapAsteroidMessage> {
        return asteroids
            .filter { clientShip.inSensorRange(it) }
            .map { it.toMapMessage() }
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

    private fun destroyTorpedo(torpedoId: ObjectId) {
        ships.values.forEach {
            it.targetDestroyed(torpedoId)
        }
        torpedoes.remove(torpedoId)
        physicsEngine.removeObject(torpedoId)
    }
}

fun CoroutineScope.gameStateActor(): SendChannel<GameStateChange> =
    Channel<GameStateChange>().also {
        startConsumer(it)
    }

private fun CoroutineScope.startConsumer(channel: Channel<GameStateChange>) {
    launch {
        var gameState = GameState()
        for (change in channel) {
            when (change) {
                is Restart -> gameState = GameState()
                is Update -> gameState.update()
                is TogglePause -> gameState.togglePaused()
                is GetGameStateSnapshot -> change.response.complete(gameState.toMessage(change.clientId))
                is NewGameClient -> gameState.clientConnected(change.clientId)
                is GameClientDisconnected -> gameState.clientDisconnected(change.clientId)
                is JoinShip -> gameState.joinShip(change.clientId, change.objectId, change.station)
                is ChangeStation -> gameState.changeStation(change.clientId, change.station)
                is ExitShip -> gameState.exitShip(change.clientId)
                is SpawnPlayerShip -> gameState.spawnPlayerShip()
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
                is DecreaseShieldModulation -> gameState.decreaseShieldModulation(change.clientId)
                is IncreaseShieldModulation -> gameState.increaseShieldModulation(change.clientId)
                is DecreaseBeamModulation -> gameState.decreaseBeamModulation(change.clientId)
                is IncreaseBeamModulation -> gameState.increaseBeamModulation(change.clientId)
                is ReloadTube -> gameState.reloadTube(change.clientId, change.index)
                is LaunchTube -> gameState.launchTube(change.clientId, change.index)
            }
        }
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
