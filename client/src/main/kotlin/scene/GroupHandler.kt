package scene

import de.stefanbissell.starcruiser.Identifiable
import de.stefanbissell.starcruiser.ObjectId
import three.core.Object3D
import three.plusAssign
import three.scenes.Scene

interface ObjectGroup {
    val rootNode: Object3D
}

class GroupHandler<G : ObjectGroup, M : Identifiable>(
    private val scene: Scene,
    private val factory: (M) -> G,
    private val update: G.(M) -> Unit
) {

    val nodes = mutableMapOf<ObjectId, G>()
    private val holder = Object3D().also { scene += it }

    fun addNew(messages: List<M>) {
        messages.filter {
            !nodes.containsKey(it.id)
        }.forEach {
            factory(it).also { node ->
                nodes[it.id] = node
                holder.add(node)
            }
        }
    }

    fun update(messages: List<M>) {
        messages.forEach { message ->
            nodes[message.id]?.apply {
                this.update(message)
            }
        }
    }

    fun removeOld(
        messages: List<M>,
        oldIds: List<ObjectId>
    ) {
        val currentIds = messages.map { it.id }
        oldIds.filter {
            !currentIds.contains(it)
        }.forEach { id ->
            nodes.remove(id)?.also {
                holder.remove(it)
            }
        }
    }
}

fun Object3D.add(group: ObjectGroup) = add(group.rootNode)

fun Object3D.remove(group: ObjectGroup) = remove(group.rootNode)
