@file:JsQualifier("THREE")

package three.textures

import org.w3c.dom.HTMLCanvasElement

open external class Texture(
    image: HTMLCanvasElement
) {

    var needsUpdate: Boolean
}
