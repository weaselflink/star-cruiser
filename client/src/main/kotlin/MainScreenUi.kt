import de.bissell.starcruiser.*
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
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
import three.renderers.WebGLRendererParams
import three.scenes.Scene
import three.updateSize
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.math.PI

class MainScreenUi {

    private val root = document.getElementById("main-screen-ui")!! as HTMLElement
    private val topViewButton = document.querySelector(".topView")!! as HTMLButtonElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val renderer = WebGLRenderer(
        WebGLRendererParams(
            canvas = canvas,
            antialias = true
        )
    )
    private val scene = Scene()
    private val ownShip = ShipGroup().also { scene.add(it) }
    private val frontCamera = createFrontCamera().also { ownShip += it }
    private val topCamera = createTopCamera().also { ownShip += it }
    private var topView = false

    private val contactGroup = Object3D().also { scene += it }
    private var model: Group? = null
    private val contactNodes = mutableMapOf<ShipId, ShipGroup>()

    init {
        resize()

        createLights()
        loadBackground()
        loadShipModel()
    }

    fun resize() {
        val windowWidth = window.innerWidth
        val windowHeight = window.innerHeight

        renderer.setSize(windowWidth, windowHeight)
        frontCamera.updateSize(windowWidth, windowHeight)
        topCamera.updateSize(windowWidth, windowHeight)
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun draw(snapshot: SnapshotMessage.MainScreen) {
        ownShip.rotation.y = snapshot.ship.rotation

        val contacts = snapshot.contacts
        val oldContactIds = contactNodes.keys.filter { true }

        addNewContacts(contacts)
        removeOldContacts(contacts, oldContactIds)
        updateContacts(contacts)
        updateBeams(snapshot)

        if (topView) {
            renderer.render(scene, topCamera)
        } else {
            renderer.render(scene, frontCamera)
        }
    }

    private fun updateBeams(snapshot: SnapshotMessage.MainScreen) {
        ownShip.updateBeams(snapshot, snapshot.ship.beams)
        contactNodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.let {
                node.value.updateBeams(snapshot, it.beams)
            }
        }
    }

    fun toggleTopView() {
        topView = !topView
        topViewButton.removeClass("current")
        if (topView) {
            topViewButton.addClass("current")
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
}

private class ShipGroup {

    val rootNode = Object3D()
    private val beamNodes = mutableListOf<Object3D>()

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

    operator fun plusAssign(value: Object3D) = rootNode.add(value)

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
                val distance = targetPosition.clone().sub(getWorldPosition(Vector3())).length()
                add(LaserBeam(length = distance, width = 2.0).obj)
            }
        }
    }

    private fun SnapshotMessage.MainScreen.getTargetPosition(targetId: ShipId?): Vector2? {
        return contacts.firstOrNull { it.id == targetId }?.relativePosition
            ?: if (ship.id == targetId) Vector2() else null
    }
}

private fun Vector2.toWorld() = Vector3(-y, 0.0, -x)

private fun Object3D.add(shipGroup: ShipGroup) = add(shipGroup.rootNode)

private fun Object3D.remove(shipGroup: ShipGroup) = remove(shipGroup.rootNode)
