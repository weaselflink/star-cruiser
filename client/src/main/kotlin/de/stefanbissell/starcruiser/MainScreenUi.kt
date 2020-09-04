package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.ShortRangeScope
import de.stefanbissell.starcruiser.components.StationUi
import de.stefanbissell.starcruiser.scene.MainScene
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import three.cameras.Camera
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams

class MainScreenUi : StationUi(Station.MainScreen) {

    private val canvas3d = document.body!!.querySelector(".canvas3d") as HTMLCanvasElement
    private val renderer = WebGLRenderer(
        WebGLRendererParams(
            canvas = canvas3d,
            antialias = true
        )
    )
    private val mainScene = MainScene()
    private val shortRangeScope = ShortRangeScope(
        canvas = canvas,
        showRotateButton = false
    )
    private var view = MainScreenView.Front
    private val frontViewButton = createViewButton(
        mainScreenView = MainScreenView.Front,
        xExpr = {
            if (it.width >= it.vmin * 152) {
                it.width * 0.5 - it.vmin * 74
            } else {
                it.width * 0.5 - it.vmin * 48
            }
        },
        yExpr = {
            if (it.width >= it.vmin * 152) {
                it.height - it.vmin * 3
            } else {
                it.height - it.vmin * 15
            }
        }
    )
    private val leftViewButton = createViewButton(
        mainScreenView = MainScreenView.Left,
        xExpr = {
            if (it.width >= it.vmin * 152) {
                it.width * 0.5 - it.vmin * 49
            } else {
                it.width * 0.5 - it.vmin * 24
            }
        },
        yExpr = {
            if (it.width >= it.vmin * 152) {
                it.height - it.vmin * 3
            } else {
                it.height - it.vmin * 15
            }
        }
    )
    private val rightViewButton = createViewButton(
        mainScreenView = MainScreenView.Right,
        xExpr = {
            if (it.width >= it.vmin * 152) {
                it.width * 0.5 - it.vmin * 24
            } else {
                it.width * 0.5 + it.vmin * 1
            }
        },
        yExpr = {
            if (it.width >= it.vmin * 152) {
                it.height - it.vmin * 3
            } else {
                it.height - it.vmin * 15
            }
        }
    )
    private val rearViewButton = createViewButton(
        mainScreenView = MainScreenView.Rear,
        xExpr = {
            if (it.width >= it.vmin * 152) {
                it.width * 0.5 + it.vmin * 1
            } else {
                it.width * 0.5 + it.vmin * 26
            }
        },
        yExpr = {
            if (it.width >= it.vmin * 152) {
                it.height - it.vmin * 3
            } else {
                it.height - it.vmin * 15
            }
        }
    )
    private val topViewButton = createViewButton(
        mainScreenView = MainScreenView.Top,
        xExpr = {
            if (it.width >= it.vmin * 152) {
                it.width * 0.5 + it.vmin * 26
            } else {
                it.width * 0.5 - it.vmin * 24
            }
        }
    )
    private val scopeViewButton = createViewButton(
        mainScreenView = MainScreenView.Scope,
        xExpr = {
            if (it.width >= it.vmin * 152) {
                it.width * 0.5 + it.vmin * 51
            } else {
                it.width * 0.5 + it.vmin * 1
            }
        }
    )

    init {
        resize()
        addChildren(
            frontViewButton,
            leftViewButton,
            rightViewButton,
            rearViewButton,
            topViewButton,
            scopeViewButton
        )
    }

    override fun resize() {
        val windowWidth = window.innerWidth
        val windowHeight = window.innerHeight

        renderer.setSize(windowWidth, windowHeight)
        mainScene.updateSize(windowWidth, windowHeight)
    }

    override fun show() {
        canvas3d.visibility = Visibility.visible
    }

    override fun hide() {
        canvas3d.visibility = Visibility.hidden
    }

    fun draw(snapshot: SnapshotMessage.MainScreen) {
        when (snapshot) {
            is SnapshotMessage.MainScreenShortRangeScope -> drawScope(snapshot)
            is SnapshotMessage.MainScreen3d -> draw3d(snapshot)
        }
    }

    fun cycleViewType() {
        clientSocket.send(Command.CommandMainScreenView(view.next))
    }

    private fun draw3d(snapshot: SnapshotMessage.MainScreen3d) {
        view = snapshot.ship.mainScreenView
        with(ctx) {
            clear()

            val camera = mainScene.update(snapshot)
            render(camera)

            drawButtons()
        }
    }

    private fun drawScope(snapshot: SnapshotMessage.MainScreenShortRangeScope) {
        view = MainScreenView.Scope
        with(ctx) {
            clearBackground()

            shortRangeScope.draw(snapshot)
            drawButtons()
        }
    }

    private fun drawButtons() {
        frontViewButton.draw()
        leftViewButton.draw()
        rightViewButton.draw()
        rearViewButton.draw()
        topViewButton.draw()
        scopeViewButton.draw()
    }

    private fun createViewButton(
        mainScreenView: MainScreenView,
        xExpr: (CanvasDimensions) -> Double,
        yExpr: (CanvasDimensions) -> Double = { it.height - it.vmin * 3 }
    ) = CanvasButton(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = yExpr,
        widthExpr = { it.vmin * 23 },
        heightExpr = { it.vmin * 10 },
        onClick = { clientSocket.send(Command.CommandMainScreenView(mainScreenView)) },
        activated = { view == mainScreenView },
        initialText = mainScreenView.name
    )

    private fun render(camera: Camera) = renderer.render(mainScene.scene, camera)
}
