package de.stefanbissell.starcruiser.scene

import three.core.Object3D
import three.math.Euler
import three.math.Quaternion
import three.math.Vector3
import three.plusAssign
import kotlin.math.sqrt
import kotlin.random.Random

class AsteroidGroup(radius: Double) : ObjectGroup {

    override val rootNode = Object3D()
    private val transformNode = Object3D().also {
        rootNode += it
        it.scale.setScalar(radius)
        it.position.y = Random.nextDouble(-8.0, 8.0)
        it.quaternion.copy(randomQuaternion())
    }

    override var model: Object3D? = null
        set(value) {
            field?.also { rootNode.remove(it) }
            field = value?.also {
                transformNode.add(it)
            }
        }

    val position: Vector3
        get() = rootNode.position

    val rotation: Euler
        get() = rootNode.rotation

    private fun randomQuaternion(): Quaternion {
        var x: Double
        var y: Double
        var z: Double
        var u: Double
        var v: Double
        var w: Double
        do {
            x = Random.nextDouble(-1.0, 1.0)
            y = Random.nextDouble(-1.0, 1.0)
            z = x * x + y * y
        } while (z > 1.0)
        do {
            u = Random.nextDouble(-1.0, 1.0)
            v = Random.nextDouble(-1.0, 1.0)
            w = u * u + v * v
        } while (w > 1.0)
        val s = sqrt((1 - z) / w)
        return Quaternion(x, y, s * u, s * v)
    }
}
