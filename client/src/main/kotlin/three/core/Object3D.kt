@file:JsQualifier("THREE")

package three.core

import three.math.Euler
import three.math.Vector3

open external class Object3D {

    var id: Number
    var name: String

    var parent: Object3D?
    var children: Array<Object3D>

    val position: Vector3
    val rotation: Euler
    val scale: Vector3

    var layers: Layers

    fun add(vararg obj: Object3D)

    fun clone(recursive: Boolean? = definedExternally): Object3D

    fun getWorldPosition(target: Vector3 ): Vector3

    fun lookAt(vector: Vector3)
    fun lookAt(x: Number, y: Number, z: Number)

    fun remove(vararg obj: Object3D)

    fun traverse(callback: (Object3D) -> Unit)
}
