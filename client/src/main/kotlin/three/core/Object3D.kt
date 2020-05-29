@file:JsQualifier("THREE")

package three.core

import three.math.Euler
import three.math.Vector3

open external class Object3D {

    var name: String

    val position: Vector3
    val rotation: Euler
    val scale: Vector3

    var children: Array<Object3D>

    fun add(vararg obj: Object3D)

    fun remove(vararg obj: Object3D)

    fun clone(recursive: Boolean? = definedExternally): Object3D
}
