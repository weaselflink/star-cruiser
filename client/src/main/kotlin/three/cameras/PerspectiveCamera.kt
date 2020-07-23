@file:JsQualifier("THREE")

package three.cameras

open external class PerspectiveCamera(
    fov: Number,
    aspect: Number,
    near: Number,
    far: Number
) : Camera {

    var aspect: Number
    var fov: Number

    fun updateProjectionMatrix()
}
