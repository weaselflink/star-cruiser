package three

import three.cameras.PerspectiveCamera
import three.core.Object3D
import three.scenes.Scene

operator fun Scene.plusAssign(obj: Object3D) = this.add(obj)

fun PerspectiveCamera.updateSize(width: Number, height: Number) {
    aspect = width.toDouble() / height.toDouble()
    updateProjectionMatrix()
}

fun Object3D.debugPrint(layer: Int = 0) {
    val prefix = (0 until layer).joinToString(separator = "") { "---- " }
    println("${prefix}name: $name")
    println(with (position) { "${prefix}position: $x,$y,$z" })
    println(with (rotation) { "${prefix}rotation: $x,$y,$z" })
    println(with (scale) { "${prefix}scale: $x,$y,$z" })
    children.forEach {
        it.debugPrint(layer + 1)
    }
}
