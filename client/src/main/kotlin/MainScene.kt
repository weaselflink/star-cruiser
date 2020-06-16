import de.bissell.starcruiser.*
import three.cameras.PerspectiveCamera
import three.core.Object3D
import three.debugPrint
import three.lights.AmbientLight
import three.lights.DirectionalLight
import three.loaders.CubeTextureLoader
import three.loaders.GLTFLoader
import three.math.Euler
import three.math.Vector3
import three.objects.Group
import three.plusAssign
import three.scenes.Scene
import three.set
import three.updateSize
import kotlin.browser.window
import kotlin.math.PI

class MainScene {

    val scene = Scene()

    private val ownShip = ShipGroup().also { scene.add(it) }
    private val contactGroup = Object3D().also { scene += it }
    private val asteroidGroup = Object3D().also { scene += it }
    private var model: Group? = null
    private var shieldModel: Group? = null
    private var asteroidModel: Group? = null
    private val contactNodes = mutableMapOf<ObjectId, ShipGroup>()
    private val asteroidNodes = mutableMapOf<ObjectId, AsteroidGroup>()

    val frontCamera = createFrontCamera().also { ownShip += it }
    val topCamera = createTopCamera().also { ownShip += it }

    init {
        createLights()
        loadBackground()
        loadShipModel()
        loadShieldModel()
        loadAsteroidModel()
    }

    fun updateSize(windowWidth: Int, windowHeight: Int) {
        frontCamera.updateSize(windowWidth, windowHeight)
        topCamera.updateSize(windowWidth, windowHeight)
    }

    fun update(snapshot: SnapshotMessage.MainScreen) {
        ownShip.rotation.y = snapshot.ship.rotation

        val contacts = snapshot.contacts
        val asteroids = snapshot.asteroids
        val oldContactIds = contactNodes.keys.filter { true }
        val oldAsteroidIds = asteroidNodes.keys.filter { true }

        addNewContacts(contacts)
        removeOldContacts(contacts, oldContactIds)
        updateContacts(contacts)

        addNewAsteroids(asteroids)
        removeOldAsteroids(asteroids, oldAsteroidIds)
        updateAsteroids(asteroids)

        updateBeams(snapshot)
        updateShields(snapshot)
    }

    private fun updateBeams(snapshot: SnapshotMessage.MainScreen) {
        ownShip.updateBeams(snapshot, snapshot.ship.beams)

        contactNodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.let { contact ->
                node.value.updateBeams(snapshot, contact.beams)
            }
        }
    }

    private fun updateShields(snapshot: SnapshotMessage.MainScreen) {
        contactNodes.values.forEach { it.hideShield() }

        if (snapshot.ship.shield.activated) {
            ownShip.showShield(snapshot.ship.shield.radius)
        } else {
            ownShip.hideShield()
        }
        contactNodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.let { contact ->
                if (contact.shield.activated) {
                    node.value.showShield(contact.shield.radius)
                } else {
                    node.value.hideShield()
                }
            }
        }
    }

    private fun addNewContacts(contacts: List<ContactMessage>) {
        contacts.filter {
            !contactNodes.containsKey(it.id)
        }.forEach {
            ShipGroup().also { node ->
                contactNodes[it.id] = node
                contactGroup.add(node)
                node.model = model?.clone(true)
                node.shieldModel = shieldModel?.clone(true)
            }
        }
    }

    private fun removeOldContacts(
        contacts: List<ContactMessage>,
        oldContactIds: List<ObjectId>
    ) {
        val currentIds = contacts.map { it.id }
        oldContactIds.filter {
            !currentIds.contains(it)
        }.forEach { id ->
            contactNodes.remove(id)?.also {
                contactGroup.remove(it)
            }
        }
    }

    private fun updateContacts(contacts: List<ContactMessage>) {
        contacts.forEach { contact ->
            contactNodes[contact.id]?.apply {
                position.copy(contact.relativePosition.toWorld())
                rotation.y = contact.rotation
            }
        }
    }

    private fun addNewAsteroids(asteroids: List<AsteroidMessage>) {
        asteroids.filter {
            !asteroidNodes.containsKey(it.id)
        }.forEach {
            AsteroidGroup(it.radius).also { node ->
                asteroidNodes[it.id] = node
                asteroidGroup.add(node)
                node.model = asteroidModel?.clone(true)
            }
        }
    }

    private fun removeOldAsteroids(
        asteroids: List<AsteroidMessage>,
        oldAsteroidIds: List<ObjectId>
    ) {
        val currentIds = asteroids.map { it.id }
        oldAsteroidIds.filter {
            !currentIds.contains(it)
        }.forEach { id ->
            asteroidNodes.remove(id)?.also {
                asteroidGroup.remove(it)
            }
        }
    }

    private fun updateAsteroids(asteroids: List<AsteroidMessage>) {
        asteroids.forEach { asteroid ->
            asteroidNodes[asteroid.id]?.apply {
                position.copy(asteroid.relativePosition.toWorld())
                rotation.y = asteroid.rotation
            }
        }
    }

    private fun createFrontCamera(): PerspectiveCamera {
        return PerspectiveCamera(
            fov = 75,
            aspect = window.innerWidth.toDouble() / window.innerHeight.toDouble(),
            near = 0.1,
            far = 10_000
        ).apply {
            position.y = 1.0
            position.z = -12.3
        }
    }

    private fun createTopCamera(): PerspectiveCamera {
        return PerspectiveCamera(
            fov = 75,
            aspect = window.innerWidth.toDouble() / window.innerHeight.toDouble(),
            near = 1,
            far = 10_000
        ).apply {
            position.y = 100.0
            rotation.x = PI * -0.5
        }
    }

    private fun createLights() {
        scene += AmbientLight(intensity = 0.25)
        scene += DirectionalLight(intensity = 4).apply {
            position.x = 5000.0
            position.y = 1000.0
        }
    }

    private fun loadBackground() {
        CubeTextureLoader().load(
            urls = backgroundUrls { "/assets/backgrounds/default/$it.png" },
            onLoad = { scene.background = it }
        )
    }

    private fun backgroundUrls(block: (String) -> String): Array<String> {
        return listOf("right", "left", "top", "bottom", "front", "back")
            .map(block)
            .toTypedArray()
    }

    private fun loadShipModel() {
        GLTFLoader().load(
            url = "/assets/models/carrier.glb",
            onLoad = { gltf ->
                model = gltf.scene.also {
                    it.debugPrint()
                }
                ownShip.model = model?.clone(true)
            }
        )
    }

    private fun loadShieldModel() {
        GLTFLoader().load(
            url = "/assets/models/shield-cube.glb",
            onLoad = { gltf ->
                shieldModel = gltf.scene.also {
                    it.debugPrint()
                }
                shieldModel?.clone(true)?.also {
                    ownShip.shieldModel = it
                }
            }
        )
    }

    private fun loadAsteroidModel() {
        GLTFLoader().load(
            url = "/assets/models/asteroid01.glb",
            onLoad = { gltf ->
                asteroidModel = gltf.scene.also {
                    it.debugPrint()
                }
            }
        )
    }

    private fun Object3D.add(group: ShipGroup) = add(group.rootNode)

    private fun Object3D.remove(group: ShipGroup) = remove(group.rootNode)

    private fun Object3D.add(group: AsteroidGroup) = add(group.rootNode)

    private fun Object3D.remove(group: AsteroidGroup) = remove(group.rootNode)
}

class ShipGroup {

    val rootNode = Object3D()
    private val beamNodes = mutableListOf<Object3D>()

    var model: Object3D? = null
        set(value) {
            field?.also { rootNode.remove(it) }
            field = value?.also {
                rootNode.add(it)
            }
        }

    var shieldModel: Object3D? = null
        set(value) {
            field?.also { rootNode.remove(it) }
            field = value?.also {
                it.visible = false
                rootNode.add(it)
            }
        }

    val position: Vector3
        get() = rootNode.position

    val rotation: Euler
        get() = rootNode.rotation

    operator fun plusAssign(value: Object3D) = rootNode.add(value)

    fun showShield(shieldRadius: Double) {
        shieldModel?.apply {
            visible = true
            scale.setScalar(shieldRadius)
        }
    }

    fun hideShield() {
        shieldModel?.visible = false
    }

    fun updateBeams(
        snapshot: SnapshotMessage.MainScreen,
        beams: List<BeamMessage>
    ) {
        beamNodes.forEach {
            rootNode.remove(it)
        }
        beamNodes.clear()

        beams.filter {
            it.status is BeamStatus.Firing
        }.forEach { beamMessage ->
            val relativePosition = snapshot.getTargetPosition(beamMessage.targetId) ?: return
            val shieldRadius = snapshot.getTargetShield(beamMessage.targetId)?.let {
                if (it.up) it.radius else 0.0
            } ?: 0.0
            val targetPosition = relativePosition.toWorld()

            Object3D().apply {
                beamNodes += this
                rootNode += this

                position.set(beamMessage.position)
                lookAt(targetPosition)
            }.apply {
                val distance = targetPosition.clone().sub(getWorldPosition(Vector3())).length() - shieldRadius
                add(LaserBeam(length = distance, width = 2.0).obj)
            }
        }
    }

    private fun SnapshotMessage.MainScreen.getTargetPosition(targetId: ObjectId?): Vector2? {
        return contacts.firstOrNull { it.id == targetId }?.relativePosition
            ?: if (ship.id == targetId) Vector2() else null
    }

    private fun SnapshotMessage.MainScreen.getTargetShield(targetId: ObjectId?): ShieldMessage? {
        return contacts.firstOrNull { it.id == targetId }?.shield
            ?: if (ship.id == targetId) ship.shield else null
    }
}

class AsteroidGroup(
    radius: Double
) {

    val rootNode = Object3D()

    var model: Object3D? = null
        set(value) {
            field?.also { rootNode.remove(it) }
            field = value?.also {
                rootNode.add(it)
            }
        }

    val position: Vector3
        get() = rootNode.position

    val rotation: Euler
        get() = rootNode.rotation

    init {
        rootNode.scale.setScalar(radius)
    }
}

private fun Vector2.toWorld() = Vector3(-y, 0.0, -x)
