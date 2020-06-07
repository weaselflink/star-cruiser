import de.bissell.starcruiser.*
import three.cameras.Camera
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
import three.renderers.WebGLRenderer
import three.scenes.Scene
import three.updateSize
import kotlin.browser.window
import kotlin.math.PI

private const val shieldRadius = 17.0

class MainScene {

    val scene = Scene()

    private val ownShip = ShipGroup().also { scene.add(it) }
    private val contactGroup = Object3D().also { scene += it }
    private var model: Group? = null
    private var shieldModel: Group? = null
    private val contactNodes = mutableMapOf<ShipId, ShipGroup>()

    val frontCamera = createFrontCamera().also { ownShip += it }
    val topCamera = createTopCamera().also { ownShip += it }

    init {
        createLights()
        loadBackground()
        loadShipModel()
        loadShieldModel()
    }

    fun updateSize(windowWidth: Int, windowHeight: Int) {
        frontCamera.updateSize(windowWidth, windowHeight)
        topCamera.updateSize(windowWidth, windowHeight)
    }

    fun update(snapshot: SnapshotMessage.MainScreen) {
        ownShip.rotation.y = snapshot.ship.rotation

        val contacts = snapshot.contacts
        val oldContactIds = contactNodes.keys.filter { true }

        addNewContacts(contacts)
        removeOldContacts(contacts, oldContactIds)
        updateContacts(contacts)
        updateBeams(snapshot)
    }

    private fun updateBeams(snapshot: SnapshotMessage.MainScreen) {
        ownShip.hideShield()
        contactNodes.values.forEach { it.hideShield() }

        ownShip.updateBeams(snapshot, snapshot.ship.beams)
        snapshot.ship.beams.filter {
            it.status is BeamStatus.Firing
        }.map {
            snapshot.getTarget(it.targetId)?.showShield()
        }

        contactNodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.let { contact ->
                node.value.updateBeams(snapshot, contact.beams)
                contact.beams.filter {
                    it.status is BeamStatus.Firing
                }.map {
                    snapshot.getTarget(it.targetId)?.showShield()
                }
            }
        }
    }

    private fun addNewContacts(contacts: List<ContactMessage>) {
        contacts.filter {
            !contactNodes.containsKey(it.id)
        }.forEach {
            ShipGroup().also { contactNode ->
                contactNodes[it.id] = contactNode
                contactGroup.add(contactNode)
                contactNode.model = model?.clone(true)
                contactNode.shieldModel = shieldModel?.clone(true)
            }
        }
    }

    private fun removeOldContacts(
        contacts: List<ContactMessage>,
        oldContactIds: List<ShipId>
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
                position.z = -contact.relativePosition.x
                position.x = -contact.relativePosition.y
                rotation.y = contact.rotation
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
            url = "/assets/ships/carrier.glb",
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
            url = "/assets/ships/shield-cube.glb",
            onLoad = { gltf ->
                shieldModel = gltf.scene.also {
                    it.scale.x = shieldRadius
                    it.scale.y = shieldRadius
                    it.scale.z = shieldRadius
                    it.debugPrint()
                }
                shieldModel?.clone(true)?.also {
                    ownShip.shieldModel = it
                }
            }
        )
    }

    private fun SnapshotMessage.MainScreen.getTarget(targetId: ShipId?): ShipGroup? =
        contactNodes[targetId] ?: if (ship.id == targetId) ownShip else null

    private fun Object3D.add(shipGroup: ShipGroup) = add(shipGroup.rootNode)

    private fun Object3D.remove(shipGroup: ShipGroup) = remove(shipGroup.rootNode)
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

    fun showShield() {
        shieldModel?.visible = true
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
            val targetPosition = relativePosition.toWorld()

            Object3D().apply {
                beamNodes += this
                rootNode += this

                position.x = beamMessage.position.x
                position.y = beamMessage.position.y
                position.z = beamMessage.position.z
                lookAt(targetPosition)
            }.apply {
                val distance = targetPosition.clone().sub(getWorldPosition(Vector3())).length() - shieldRadius
                add(LaserBeam(length = distance, width = 2.0).obj)
            }
        }
    }

    private fun SnapshotMessage.MainScreen.getTargetPosition(targetId: ShipId?): Vector2? {
        return contacts.firstOrNull { it.id == targetId }?.relativePosition
            ?: if (ship.id == targetId) Vector2() else null
    }

    private fun Vector2.toWorld() = Vector3(-y, 0.0, -x)
}

fun WebGLRenderer.render(mainScene: MainScene, camera: Camera) = render(mainScene.scene, camera)
