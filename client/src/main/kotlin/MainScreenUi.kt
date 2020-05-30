import de.bissell.starcruiser.ContactMessage
import de.bissell.starcruiser.SnapshotMessage
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
    private val shipGroup = Group().also { scene.add(it) }
    private val frontCamera = createFrontCamera().also { shipGroup.add(it) }
    private val topCamera = createTopCamera().also { shipGroup.add(it) }
    private var topView = false

    private val contactGroup = Object3D().also { scene += it }
    private var model: Group? = null
    private var ownModel: Object3D? = null
    private val contactNodes = mutableMapOf<String, Object3D>()

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
        shipGroup.rotation.y = snapshot.ship.rotation

        val contacts = snapshot.contacts
        val oldContactIds = contactNodes.keys.filter { true }

        addNewContacts(contacts)
        removeOldContacts(contacts, oldContactIds)
        updateContacts(contacts)

        if (topView) {
            renderer.render(scene, topCamera)
        } else {
            renderer.render(scene, frontCamera)
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
        oldContactIds: List<String>
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
            near = 1,
            far = 10_000
        ).apply {
            position.z = -12.5
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
            onLoad = {
                model = it.scene.apply {
                    debugPrint()
                }
                ownModel = model?.clone(true)?.also { ownModel ->
                    shipGroup.add(ownModel)
                }
            }
        )
    }
}
