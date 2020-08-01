package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.ShortRangeScope
import de.stefanbissell.starcruiser.components.StationUi
import de.stefanbissell.starcruiser.input.PointerEventDispatcher
import de.stefanbissell.starcruiser.scene.MainScene
import org.w3c.dom.HTMLCanvasElement
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
    private val shortRangeScope = ShortRangeScope(
        canvas = overlayCanvas,
        showRotateButton = false
    )
    private var view = MainScreenView.Front
    private val frontViewButton = createViewButton(
        mainScreenView = MainScreenView.Front,
        xExpr = { it.width * 0.5 - it.vmin * 58 }
    )
    private val topViewButton = createViewButton(
        mainScreenView = MainScreenView.Top,
        xExpr = { it.width * 0.5 - it.vmin * 18 }
    )
    private val scopeViewButton = createViewButton(
        mainScreenView = MainScreenView.Scope,
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
        view = snapshot.ship.mainScreenView
        when (view) {
            MainScreenView.Front -> draw3d(snapshot, mainScene.frontCamera)
            MainScreenView.Top -> draw3d(snapshot, mainScene.topCamera)
            MainScreenView.Scope -> drawScope(snapshot)
        }
    }

    private fun draw3d(snapshot: SnapshotMessage.MainScreen, camera: Camera) {
        with(ctx) {
            clear()

            mainScene.update(snapshot)
            render(camera)

            frontViewButton.draw()
            topViewButton.draw()
            scopeViewButton.draw()
        }
    }

    private fun drawScope(snapshot: SnapshotMessage.MainScreen) {
        with(ctx) {
            clearBackground()

            shortRangeScope.draw(snapshot)
            frontViewButton.draw()
            topViewButton.draw()
            scopeViewButton.draw()
        }
    }

    fun cycleViewType() {
        clientSocket.send(Command.CommandMainScreenView(view.next))
    }

    private fun createViewButton(
        mainScreenView: MainScreenView,
        xExpr: (CanvasDimensions) -> Double
    ) = CanvasButton(
        canvas = overlayCanvas,
        xExpr = xExpr,
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 36 },
        heightExpr = { it.vmin * 10 },
        onClick = { clientSocket.send(Command.CommandMainScreenView(mainScreenView)) },
        activated = { view == mainScreenView },
        initialText = mainScreenView.name
    )

    private fun render(camera: Camera) = renderer.render(mainScene.scene, camera)
}
