@file:JsQualifier("THREE")

package three.math

external class Quaternion(
    x: Number = definedExternally,
    y: Number = definedExternally,
    z: Number = definedExternally,
    w: Number = definedExternally
) {

    var x: Double
    var y: Double
    var z: Double
    var w: Double

    fun copy(q: Quaternion): Quaternion
}

