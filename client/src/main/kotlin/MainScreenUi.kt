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
    private val ownShip = ShipGroup().also { scene += it.rootNode }
    private val frontCamera = createFrontCamera().also { ownShip += it }
    private val topCamera = createTopCamera().also { ownShip += it }
    private var topView = false

    private val contactGroup = Object3D().also { scene += it }
    private val beamGroup = Object3D().also { ownShip += it }
    private var model: Group? = null
    private val contactNodes = mutableMapOf<ShipId, Object3D>()

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
        beamGroup.remove(*beamGroup.children)

        val lockProgress = snapshot.ship.lockProgress
        if (lockProgress !is LockStatus.Locked) return
        val target = snapshot.contacts.firstOrNull { it.id == lockProgress.targetId } ?: return
        val targetPosition = Vector3(-target.relativePosition.y, 0.0, -target.relativePosition.x)

        snapshot.ship.beams.filter {
            it.status is BeamStatus.Firing
        }.forEach { beamMessage ->
            val distance = (targetPosition - beamMessage.position).length()
            Object3D().apply {
                beamGroup += this

                position.x = beamMessage.position.x
                position.y = beamMessage.position.y
                position.z = beamMessage.position.z
                lookAt(
                    x = targetPosition.x,
                    y = targetPosition.y,
                    z = targetPosition.z
                )

                add(LaserBeam(length = distance, width = 2.0).obj)
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
            Object3D().also { contactNode ->
                contactNodes[it.id] = contactNode
                contactGroup.add(contactNode)
                model?.clone(true)?.also { contactModel ->
                    contactNode.add(contactModel)
                }
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

    inner class ShipGroup {

        val rootNode = Object3D()

        var model: Object3D? = null
            set(value) {
                field?.also { rootNode.remove(it) }
                field = value?.also {
                    rootNode.add(it)
                }
            }

        val rotation: Euler
            get() = rootNode.rotation

        operator fun plusAssign(value: Object3D) = rootNode.add(value)
    }
}
