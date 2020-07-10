package de.stefanbissell.starcruiser.scene

import de.stefanbissell.starcruiser.BeamMessage
import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Vector2
import three.core.Object3D
import three.math.Euler
import three.math.Vector3
import three.plusAssign
import three.set

class ShipGroup : ObjectGroup {

    override val rootNode = Object3D()
    private val beamNodes = mutableListOf<Object3D>()

    var model: Object3D? = null
        set(value) {
            field?.also { rootNode.remove(it) }
            field = value?.also {
                rootNode.add(it)
            }
        }

    var shieldModel: Object3D? = null
        set(value) {
            field?.also { rootNode.remove(it) }
            field = value?.also {
                it.visible = false
                rootNode.add(it)
            }
        }

    val position: Vector3
        get() = rootNode.position

    val rotation: Euler
        get() = rootNode.rotation

    val scale: Vector3
        get() = rootNode.scale

    operator fun plusAssign(value: Object3D) = rootNode.add(value)

    fun showShield(shieldRadius: Double) {
        shieldModel?.apply {
            visible = true
            scale.setScalar(shieldRadius)
        }
    }

    fun hideShield() {
        shieldModel?.visible = false
    }

    fun updateBeams(
        snapshot: SnapshotMessage.MainScreen,
        beams: List<BeamMessage>
    ) {
        beamNodes.forEach {
            rootNode.remove(it)
        }
        beamNodes.clear()

        beams.filter {
            it.status is BeamStatus.Firing
        }.forEach {
            updateBeam(snapshot, it)
        }
    }

    private fun updateBeam(snapshot: SnapshotMessage.MainScreen, beamMessage: BeamMessage) {
        val relativePosition = snapshot.getTargetPosition(beamMessage.targetId) ?: return
        val shieldRadius = snapshot.getTargetShield(beamMessage.targetId)?.let {
            if (it.up) it.radius else 0.0
        } ?: 0.0
        val targetPosition = relativePosition.toWorld()

        createBeamGroup(beamMessage, targetPosition, shieldRadius)
    }

    private fun createBeamGroup(beamMessage: BeamMessage, targetPosition: Vector3, shieldRadius: Double) {
        Object3D().apply {
            beamNodes += this
            rootNode += this

            position.set(beamMessage.position)
            lookAt(targetPosition)
        }.apply {
            val distance = targetPosition.clone().sub(getWorldPosition(Vector3())).length() - shieldRadius
            add(LaserBeam(length = distance, width = 2.0).obj)
        }
    }

    private fun SnapshotMessage.MainScreen.getTargetPosition(targetId: ObjectId?): Vector2? {
        return longRangeContacts.firstOrNull { it.id == targetId }?.relativePosition
            ?: if (ship.id == targetId) Vector2() else null
    }

    private fun SnapshotMessage.MainScreen.getTargetShield(targetId: ObjectId?): ShieldMessage? {
        return longRangeContacts.firstOrNull { it.id == targetId }?.shield
            ?: if (ship.id == targetId) ship.shield else null
    }
}
