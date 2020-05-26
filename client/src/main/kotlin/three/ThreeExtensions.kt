package three

import three.cameras.PerspectiveCamera
import three.core.Object3D
import three.scenes.Scene

operator fun Scene.plusAssign(obj: Object3D) = this.add(obj)

fun PerspectiveCamera.updateSize(width: Number, height: Number) {
    aspect = width.toDouble() / height.toDouble()
    updateProjectionMatrix()
}