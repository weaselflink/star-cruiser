import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.Station.Helm
import de.bissell.starcruiser.Station.Navigation
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import three.cameras.PerspectiveCamera
import three.core.Object3D
import three.lights.AmbientLight
import three.lights.DirectionalLight
import three.loaders.GLTFLoader
import three.objects.Group
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams
import three.scenes.Scene
import kotlin.browser.document
import kotlin.browser.window

class MainScreenUi {

    private val root = document.getElementById("main-screen")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val renderer = WebGLRenderer(
        WebGLRendererParams(
            canvas = canvas,
            antialias = true
        )
    )
    private val scene = Scene()
    private val camera = PerspectiveCamera(
        fov = 75,
        aspect = window.innerWidth.toDouble() / window.innerHeight.toDouble(),
        near = 1,
        far = 10_000
    )
    private val contactGroup = Object3D().also { scene.add(it) }
    private var model: Group? = null
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement
    private val fullScreenButton = root.querySelector(".fullscreen")!! as HTMLButtonElement
    private val toHelmButton = root.querySelector(".switchToHelm")!! as HTMLButtonElement
    private val toNavigationButton = root.querySelector(".switchToNavigation")!! as HTMLButtonElement

    init {
        resize()
        exitButton.onclick = { clientSocket.send(Command.CommandExitShip) }
        fullScreenButton.onclick = {
            val body = document.querySelector("body")!! as HTMLElement
            if (document.fullscreenElement == null) {
                body.requestFullscreen()
                fullScreenButton.innerText = "Windowed"
            } else {
                document.exitFullscreen()
                fullScreenButton.innerText = "Fullscreen"
            }
        }
        toHelmButton.onclick = { clientSocket.send(Command.CommandChangeStation(Helm)) }
        toNavigationButton.onclick = { clientSocket.send(Command.CommandChangeStation(Navigation)) }

        val ambientLight = AmbientLight(intensity = 0.25)
        scene.add(ambientLight)
        val directionalLight = DirectionalLight(intensity = 4).apply {
            position.x = 5.0
            position.y = 1.0
        }
        scene.add(directionalLight)

        GLTFLoader().load(
            url = "/assets/carrier.glb",
            onLoad = {
                model = it.scene
            }
        )
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

        contactGroup.remove(*contactGroup.children)
        stateCopy.snapshot.contacts.mapNotNull {

            model?.clone(true)?.apply {
                position.z = -it.relativePosition.x
                position.x = -it.relativePosition.y
                rotation.y = it.rotation
            }
        }.forEach {
            contactGroup.add(it)
        }

        renderer.render(scene, camera)
    }
}
