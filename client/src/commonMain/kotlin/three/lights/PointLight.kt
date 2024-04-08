@file:JsQualifier("THREE")

package three.lights

import three.core.Color

external class PointLight(
    color: Color? = definedExternally,
    intensity: Number? = definedExternally,
    distance: Number? = definedExternally,
    decay: Number? = definedExternally
) : Light
