package three

import three.cameras.PerspectiveCamera
import three.core.Object3D
import three.math.Vector3

operator fun Object3D.plusAssign(obj: Object3D) = add(obj)

operator fun Object3D.minusAssign(obj: Object3D) = remove(obj)

fun PerspectiveCamera.updateSize(width: Number, height: Number) {
    aspect = width.toDouble() / height.toDouble()
    updateProjectionMatrix()
}

fun Vector3.set(v: de.stefanbissell.starcruiser.Vector3) = set(v.x, v.y, v.z)
