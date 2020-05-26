import de.bissell.starcruiser.ContactMessage
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.ShipMessage
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import three.cameras.PerspectiveCamera
import three.core.Object3D
import three.lights.AmbientLight
import three.lights.DirectionalLight
import three.loaders.CubeTextureLoader
import three.loaders.GLTFLoader
import three.objects.Group
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams
import three.scenes.Scene
import kotlin.browser.document
import kotlin.browser.window

class MainScreenUi {

    private val root = document.getElementById("main-screen-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val renderer = WebGLRenderer(
        WebGLRendererParams(
            canvas = canvas,
            antialias = true
        )
    )
    private val scene = Scene()
    private val camera = createCamera()

    private val contactGroup = Object3D().also { scene.add(it) }
    private var model: Group? = null
    private val contactModels = mutableMapOf<String, Object3D>()

    init {
        resize()

        createLights()
        loadBackground()
        loadShipModel()
    }

    fun resize() {
        val windowWidth: Int = window.innerWidth
        val windowHeight: Int = window.innerHeight

        renderer.setSize(windowWidth, windowHeight)
        camera.aspect = windowWidth.toDouble() / windowHeight.toDouble()
        camera.updateProjectionMatrix()
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun draw(ship: ShipMessage, stateCopy: GameStateMessage) {
        camera.rotation.y = ship.rotation

        val contacts = stateCopy.snapshot.contacts
        val oldContactIds = contactModels.keys.filter { true }

        addNewContacts(contacts)
        removeOldContacts(contacts, oldContactIds)
        updateContacts(contacts)

        renderer.render(scene, camera)
    }

    private fun addNewContacts(contacts: List<ContactMessage>) {
        contacts.filter {
            !contactModels.containsKey(it.id)
        }.forEach {
            model?.clone(true)?.also { contactModel ->
                contactModels[it.id] = contactModel
                contactGroup.add(contactModel)
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
            contactModels.remove(id)?.also {
                contactGroup.remove(it)
            }
        }
    }

    private fun updateContacts(contacts: List<ContactMessage>) {
        contacts.forEach { contact ->
            contactModels[contact.id]?.apply {
                position.z = -contact.relativePosition.x
                position.x = -contact.relativePosition.y
                rotation.y = contact.rotation
            }
        }
    }

    private fun createCamera(): PerspectiveCamera {
        return PerspectiveCamera(
            fov = 75,
            aspect = window.innerWidth.toDouble() / window.innerHeight.toDouble(),
            near = 1,
            far = 10_000
        )
    }

    private fun createLights() {
        val ambientLight = AmbientLight(intensity = 0.25)
        scene.add(ambientLight)
        val directionalLight = DirectionalLight(intensity = 4).apply {
            position.x = 5.0
            position.y = 1.0
        }
        scene.add(directionalLight)
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
            onLoad = { model = it.scene }
        )
    }
}
