package three.renderers

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.RenderingContext

data class WebGLRendererParams(
    var canvas: HTMLCanvasElement? = undefined,
    var context: RenderingContext? = undefined,
    var antialias: Boolean = false
)
