@file:JsQualifier("THREE")

package three.math

external class Vector3(
    x: Number = definedExternally,
    y: Number = definedExternally,
    z: Number = definedExternally
) {

    var x: Double
    var y: Double
    var z: Double

    fun add(a: Vector3): Vector3

    fun clone(): Vector3

    fun copy(v: Vector3): Vector3

    fun length() : Double

    fun set(x: Number, y: Number, z: Number): Vector3

    fun setScalar(scalar: Number): Vector3

    fun sub(a: Vector3): Vector3
}
