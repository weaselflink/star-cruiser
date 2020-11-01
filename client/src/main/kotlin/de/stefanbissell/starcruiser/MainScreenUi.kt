package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.ShortRangeScope
import de.stefanbissell.starcruiser.components.StationUi
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.scene.MainScene
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.ROUND
import three.cameras.Camera
import three.renderers.WebGLRenderer
import three.renderers.WebGLRendererParams
import kotlin.math.PI

class MainScreenUi : StationUi(Station.MainScreen) {

    private val canvas3d = document.canvas3d
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
    private val viewButtons = MainScreenView.values()
        .mapIndexed { index, view ->
            createViewButton(
                mainScreenView = view,
                xExpr = {
                    val buttonWidth = vmin * 22
                    val gapWidth = vmin * 2
                    val combinedWidth = buttonWidth + gapWidth
                    val requiredFull = buttonWidth * 6 + gapWidth * 7
                    if (width >= requiredFull) {
                        width * 0.5 - combinedWidth * 3 + gapWidth * 0.5 + index * combinedWidth
                    } else {
                        if (index < 4) {
                            width * 0.5 - combinedWidth * 2 + gapWidth * 0.5 + index * combinedWidth
                        } else {
                            width * 0.5 - combinedWidth + gapWidth * 0.5 + (index - 4) * combinedWidth
                        }
                    }
                },
                yExpr = {
                    val buttonWidth = vmin * 22
                    val gapWidth = vmin * 2
                    val requiredFull = buttonWidth * 6 + gapWidth * 7
                    if (width >= requiredFull || index >= 4) {
                        height - vmin * 3
                    } else {
                        height - vmin * 15
                    }
                }
            )
        }

    init {
        resize()
        addChildren(viewButtons)
        addChildren(ScreenPointerHandler())
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

    private fun draw3d(snapshot: SnapshotMessage.MainScreen3d) {
        view = snapshot.ship.mainScreenView
        with(ctx) {
            clear()

            val camera = mainScene.update(snapshot)
            render(camera)

            if (snapshot.ship.mainScreenView != MainScreenView.Top) {
                drawViewIndicator(snapshot)
            }
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

    private fun CanvasRenderingContext2D.drawViewIndicator(snapshot: SnapshotMessage.MainScreen3d) {
        val dim = canvas.dimensions()
        val width = dim.width
        val vmin = dim.vmin
        val center = Vector2(width * 0.5, vmin * 7)

        save()

        translate(center)

        when (snapshot.ship.mainScreenView) {
            MainScreenView.Right -> rotate(PI * 0.5)
            MainScreenView.Rear -> rotate(PI)
            MainScreenView.Left -> rotate(PI * 1.5)
            else -> Unit
        }

        lineWidth = vmin * 0.3
        lineJoin = CanvasLineJoin.ROUND
        strokeStyle = "#555"

        beginPath()
        circle(Vector2(), vmin * 6)
        stroke()

        strokeStyle = "#fff"

        beginPath()
        moveTo(Vector2())
        lineTo(Vector2(0, vmin * 6).rotate(PI + PI * 0.25))
        arc(0.0, 0.0, vmin * 6, -PI * 0.25, -PI * 0.75, true)
        closePath()
        stroke()

        restore()
    }

    private fun drawButtons() {
        viewButtons.forEach {
            it.draw()
        }
    }

    private fun createViewButton(
        mainScreenView: MainScreenView,
        xExpr: CanvasDimensions.() -> Double,
        yExpr: CanvasDimensions.() -> Double = { height - vmin * 3 }
    ) = CanvasButton(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = yExpr,
        widthExpr = { vmin * 22 },
        heightExpr = { vmin * 10 },
        onClick = { ClientSocket.send(Command.CommandMainScreenView(mainScreenView)) },
        activated = { view == mainScreenView },
        initialText = mainScreenView.name
    )

    private fun render(camera: Camera) = renderer.render(mainScene.scene, camera)
}

private class ScreenPointerHandler : PointerEventHandler {

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        ClientState.showStationOverlay = true
    }
}
