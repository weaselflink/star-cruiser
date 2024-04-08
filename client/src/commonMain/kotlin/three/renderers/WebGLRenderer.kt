@file:JsQualifier("THREE")

package three.renderers

import three.cameras.Camera
import three.scenes.Scene

open external class WebGLRenderer(
    parameters: WebGLRendererParams = definedExternally
) {

    fun setSize(
        width: Int,
        height: Int,
        updateStyle: Boolean = definedExternally
    )

    fun render(
        scene: Scene,
        camera: Camera,
        renderTarget: dynamic = definedExternally,
        forceClear: Boolean = definedExternally
    )
}
