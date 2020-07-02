import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import org.w3c.dom.HTMLButtonElement
import scene.MainScene
import three.cameras.Camera
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass
import kotlin.dom.removeClass

class MainScreenUi : StationUi {

    override val station = Station.MainScreen

    private val root = document.getHtmlElementById("main-screen-ui")
    private val canvas = root.canvas
    private val topViewButton: HTMLButtonElement = document.byQuery(".topView")
    private val renderer = WebGLRenderer(
        WebGLRendererParams(
            canvas = canvas,
            antialias = true
        )
    )
    private val mainScene = MainScene()
    private var topView = false

    init {
        resize()
    }

    fun resize() {
        val windowWidth = window.innerWidth
        val windowHeight = window.innerHeight

        renderer.setSize(windowWidth, windowHeight)
        mainScene.updateSize(windowWidth, windowHeight)
    }

    override fun show() {
        root.visibility = Visibility.visible
    }

    override fun hide() {
        root.visibility = Visibility.hidden
    }

    fun draw(snapshot: SnapshotMessage.MainScreen) {
        mainScene.update(snapshot)

        if (topView) {
            renderer.render(mainScene, mainScene.topCamera)
        } else {
            renderer.render(mainScene, mainScene.frontCamera)
        }
    }

    fun toggleTopView() {
        topView = !topView
        topViewButton.removeClass("current")
        if (topView) {
            topViewButton.addClass("current")
        }
    }
}

private fun WebGLRenderer.render(mainScene: MainScene, camera: Camera) = render(mainScene.scene, camera)
