@file:JsQualifier("THREE")

package three.lights

import three.core.Color

external class DirectionalLight(
    color: Color? = definedExternally,
    intensity: Number? = definedExternally
) : Light
