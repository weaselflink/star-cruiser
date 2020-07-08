import components.CanvasButton
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import input.PointerEventDispatcher
import org.w3c.dom.HTMLCanvasElement
import scene.MainScene
import three.cameras.Camera
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams
import kotlin.browser.document
import kotlin.browser.window

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
    private var viewType = ViewType.Front
    private val frontViewButton = createViewButton(
        view = ViewType.Front,
        xExpr = { it.width * 0.5 - it.vmin * 58 }
    )
    private val topViewButton = createViewButton(
        view = ViewType.Top,
        xExpr = { it.width * 0.5 - it.vmin * 18 }
    )
    private val scopeViewButton = createViewButton(
        view = ViewType.Scope,
        xExpr = { it.width * 0.5 + it.vmin * 22 }
    )

    init {
        resize()
        pointerEventDispatcher.addHandlers(
            frontViewButton,
            topViewButton,
            scopeViewButton
        )
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

        when (viewType) {
            ViewType.Front -> renderer.render(mainScene, mainScene.frontCamera)
            else -> renderer.render(mainScene, mainScene.frontCamera)
        }

        with(ctx) {
            clear("#0000")

            frontViewButton.draw()
            topViewButton.draw()
            scopeViewButton.draw()
        }
    }

    fun cycleViewType() {
        viewType = viewType.next
    }

    private fun createViewButton(
        view: ViewType,
        xExpr: (CanvasDimensions) -> Double
    ) = CanvasButton(
        canvas = overlayCanvas,
        xExpr = xExpr,
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 36 },
        heightExpr = { it.vmin * 10 },
        onClick = { viewType = view },
        activated = { viewType == view },
        text = { view.name }
    )

}

private fun WebGLRenderer.render(mainScene: MainScene, camera: Camera) = render(mainScene.scene, camera)

private enum class ViewType {
    Front,
    Top,
    Scope;

    val next: ViewType
        get() = when (this) {
            Front -> Top
            Top -> Scope
            Scope -> Front
        }
}