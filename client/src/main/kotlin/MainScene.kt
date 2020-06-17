import de.bissell.starcruiser.AsteroidMessage
import de.bissell.starcruiser.BeamMessage
import de.bissell.starcruiser.BeamStatus
import de.bissell.starcruiser.ContactMessage
import de.bissell.starcruiser.Identifiable
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.ShieldMessage
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Vector2
import three.cameras.PerspectiveCamera
import three.core.Object3D
import three.debugPrint
import three.lights.AmbientLight
import three.lights.DirectionalLight
import three.loaders.CubeTextureLoader
import three.loaders.GLTFLoader
import three.math.Euler
import three.math.Quaternion
import three.math.Vector3
import three.objects.Group
import three.plusAssign
import three.scenes.Scene
import three.set
import three.updateSize
import kotlin.browser.window
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random

class MainScene {

    val scene = Scene()

    private val ownShip = ShipGroup().also { scene.add(it) }
    private val contactHandler = GroupHandler<ShipGroup, ContactMessage>(
        factory = {
            ShipGroup().also { node ->
                node.model = model?.clone(true)
                node.shieldModel = shieldModel?.clone(true)
            }
        },
        update = { contact ->
            position.copy(contact.relativePosition.toWorld())
            rotation.y = contact.rotation
        }
    )
    private val asteroidHandler = GroupHandler<AsteroidGroup, AsteroidMessage>(
        factory = { message ->
            AsteroidGroup(message.radius).also { node ->
                node.model = asteroidModel?.clone(true)
            }
        },
        update = { asteroid ->
            position.copy(asteroid.relativePosition.toWorld())
            rotation.y = asteroid.rotation
        }
    )
    private var model: Group? = null
    private var shieldModel: Group? = null
    private var asteroidModel: Group? = null

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
        val oldContactIds = contactHandler.nodes.keys.filter { true }
        val oldAsteroidIds = asteroidHandler.nodes.keys.filter { true }

        with(contactHandler) {
            addNew(contacts)
            removeOld(contacts, oldContactIds)
            update(contacts)
        }
        with(asteroidHandler) {
            addNew(asteroids)
            removeOld(asteroids, oldAsteroidIds)
            update(asteroids)
        }

        updateBeams(snapshot)
        updateShields(snapshot)
    }

    private fun updateBeams(snapshot: SnapshotMessage.MainScreen) {
        ownShip.updateBeams(snapshot, snapshot.ship.beams)

        contactHandler.nodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.let { contact ->
                node.value.updateBeams(snapshot, contact.beams)
            }
        }
    }

    private fun updateShields(snapshot: SnapshotMessage.MainScreen) {
        contactHandler.nodes.values.forEach { it.hideShield() }

        if (snapshot.ship.shield.activated) {
            ownShip.showShield(snapshot.ship.shield.radius)
        } else {
            ownShip.hideShield()
        }
        contactHandler.nodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.let { contact ->
                if (contact.shield.activated) {
                    node.value.showShield(contact.shield.radius)
                } else {
                    node.value.hideShield()
                }
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
        loadModel("carrier.glb") { group ->
            model = group
            group.debugPrint()
            assignModel(group, { model }, { model = it })
        }
    }

    private fun loadShieldModel() {
        loadModel("shield-cube.glb") { group ->
            shieldModel = group
            group.debugPrint()
            assignModel(group, { shieldModel }, { shieldModel = it })
        }
    }

    private fun assignModel(
        group: Group,
        getter: ShipGroup.() -> Object3D?,
        setter: ShipGroup.(Object3D) -> Unit
    ) {
        if (ownShip.getter() == null) {
            ownShip.setter(group.clone(true))
        }
        contactHandler.nodes.values.forEach {
            if (it.getter() == null) {
                it.setter(group.clone(true))
            }
        }
    }

    private fun loadAsteroidModel() {
        loadModel("asteroid01.glb") { group ->
            asteroidModel = group
            group.debugPrint()
            asteroidHandler.nodes.values.forEach {
                if (it.model == null) {
                    it.model = group.clone(true)
                }
            }
        }
    }

    private fun loadModel(name: String, onLoad: (Group) -> Unit) {
        GLTFLoader().load(
            url = "/assets/models/$name",
            onLoad = { onLoad(it.scene) }
        )
    }

    private fun Object3D.add(group: ObjectGroup) = add(group.rootNode)

    private fun Object3D.remove(group: ObjectGroup) = remove(group.rootNode)

    inner class GroupHandler<G : ObjectGroup, M : Identifiable>(
        val factory: (M) -> G,
        val update: G.(M) -> Unit
    ) {

        val nodes = mutableMapOf<ObjectId, G>()
        private val holder = Object3D().also { scene += it }

        fun addNew(messages: List<M>) {
            messages.filter {
                !nodes.containsKey(it.id)
            }.forEach {
                factory(it).also { node ->
                    nodes[it.id] = node
                    holder.add(node)
                }
            }
        }

        fun update(messages: List<M>) {
            messages.forEach { message ->
                nodes[message.id]?.apply {
                    this.update(message)
                }
            }

        }

        fun removeOld(
            messages: List<M>,
            oldIds: List<ObjectId>
        ) {
            val currentIds = messages.map { it.id }
            oldIds.filter {
                !currentIds.contains(it)
            }.forEach { id ->
                nodes.remove(id)?.also {
                    holder.remove(it)
                }
            }
        }
    }
}

interface ObjectGroup {
    val rootNode: Object3D
}

class ShipGroup : ObjectGroup {

    override val rootNode = Object3D()
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

class AsteroidGroup(radius: Double) : ObjectGroup {

    override val rootNode = Object3D()
    private val transformNode = Object3D().also {
        rootNode += it
        it.scale.setScalar(radius)
        it.position.y = Random.nextDouble(-8.0, 8.0)
        it.quaternion.copy(randomQuaternion())
    }

    var model: Object3D? = null
        set(value) {
            field?.also { rootNode.remove(it) }
            field = value?.also {
                transformNode.add(it)
            }
        }

    val position: Vector3
        get() = rootNode.position

    val rotation: Euler
        get() = rootNode.rotation
}

private fun Vector2.toWorld() = Vector3(-y, 0.0, -x)

private fun randomQuaternion(): Quaternion {
    var x: Double
    var y: Double
    var z: Double
    var u: Double
    var v: Double
    var w: Double
    do {
        x = Random.nextDouble(-1.0, 1.0)
        y = Random.nextDouble(-1.0, 1.0)
        z = x * x + y * y
    } while (z > 1.0)
    do {
        u = Random.nextDouble(-1.0, 1.0)
        v = Random.nextDouble(-1.0, 1.0)
        w = u * u + v * v
    } while (w > 1.0)
    val s = sqrt((1 - z) / w)
    return Quaternion(x, y, s * u, s * v)
}
