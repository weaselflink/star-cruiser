import components.CanvasButton
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import input.PointerEventDispatcher
import kotlin.browser.document
import kotlin.browser.window
import org.w3c.dom.HTMLCanvasElement
import scene.MainScene
import three.cameras.Camera
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams

class MainScreenUi : StationUi {

    override val station = Station.MainScreen

    private val root = document.getHtmlElementById("main-screen-ui")
    private val canvas: HTMLCanvasElement = root.byQuery(".canvas3d")
    private val overlayCanvas: HTMLCanvasElement = root.byQuery(".canvas2d")
    private val ctx = overlayCanvas.context2D
    private val renderer = WebGLRenderer(
        WebGLRendererParams(
            canvas = canvas,
            antialias = true
        )
    )
    private val mainScene = MainScene()
    private val pointerEventDispatcher = PointerEventDispatcher(overlayCanvas)
    private var topView = false
    private val frontViewButton = CanvasButton(
        canvas = overlayCanvas,
        xExpr = { it.width * 0.5 - it.vmin * 39 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 37 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleTopView() },
        activated = { topView.not() },
        text = { "Front" }
    )
    private val topViewButton = CanvasButton(
        canvas = overlayCanvas,
        xExpr = { it.width * 0.5 + it.vmin * 2 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 37 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleTopView() },
        activated = { topView },
        text = { "Top" }
    )

    init {
        resize()
        pointerEventDispatcher.addHandler(frontViewButton)
        pointerEventDispatcher.addHandler(topViewButton)
    }

    fun resize() {
        val windowWidth = window.innerWidth
        val windowHeight = window.innerHeight

        renderer.setSize(windowWidth, windowHeight)
        mainScene.updateSize(windowWidth, windowHeight)
        overlayCanvas.updateSize()
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

        with(ctx) {
            clear("#0000")

            frontViewButton.draw()
            topViewButton.draw()
        }
    }

    fun toggleTopView() {
        topView = !topView
    }
}

private fun WebGLRenderer.render(mainScene: MainScene, camera: Camera) = render(mainScene.scene, camera)
