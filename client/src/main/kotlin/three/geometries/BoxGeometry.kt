@file:JsQualifier("THREE")

package three.geometries

import three.core.Geometry

external class BoxGeometry(
    width: Number = definedExternally,
    height: Number = definedExternally,
    depth: Number = definedExternally,
    widthSegments: Int = definedExternally,
    heightSegments: Int = definedExternally,
    depthSegments: Int = definedExternally
) : Geometry
