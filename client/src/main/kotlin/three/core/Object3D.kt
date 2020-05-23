@file:JsQualifier("THREE")

package three.core

import three.math.Euler
import three.math.Vector3

open external class Object3D {

    val position: Vector3
    val rotation: Euler

    fun add(vararg obj: Object3D)
}
