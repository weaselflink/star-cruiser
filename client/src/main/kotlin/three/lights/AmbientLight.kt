@file:JsQualifier("THREE")

package three.lights

import three.core.Color

external class AmbientLight(
    color: Color? = definedExternally,
    intensity: Number? = definedExternally
) : Light
