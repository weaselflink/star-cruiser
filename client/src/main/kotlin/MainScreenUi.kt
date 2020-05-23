import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.Station.Helm
import de.bissell.starcruiser.Station.Navigation

import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import three.cameras.PerspectiveCamera
import three.core.Color
import three.geometries.BoxGeometry
import three.materials.MaterialParameters
import three.materials.MeshBasicMaterial
import three.objects.Mesh
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams
import three.scenes.Scene
import kotlin.browser.document
import kotlin.browser.window

class MainScreenUi {

    private val root = document.getElementById("main-screen")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val renderer = WebGLRenderer(WebGLRendererParams(canvas))
    private val scene = Scene()
    private val camera = PerspectiveCamera(
        fov = 75,
        aspect = window.innerWidth.toDouble() / window.innerHeight.toDouble(),
        near = 0.1,
        far = 1000
    )
    private val cube: Mesh
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

        val geometry = BoxGeometry()
        val material = MeshBasicMaterial(MaterialParameters(color = Color(0x5f9ea0), wireframe = true))
        cube = Mesh(geometry, material)
        scene.add(cube)

        camera.position.z = 5.0
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
        cube.rotation.x += 0.01
        cube.rotation.y += 0.01

        renderer.render(scene, camera)
    }
}
