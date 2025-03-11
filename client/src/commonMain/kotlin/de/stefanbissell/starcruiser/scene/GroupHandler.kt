package de.stefanbissell.starcruiser.scene

import de.stefanbissell.starcruiser.IdentifiableWithModel
import de.stefanbissell.starcruiser.ObjectId
import three.core.Object3D
import three.objects.Group
import three.plusAssign
import three.scenes.Scene

interface ObjectGroup {
    val rootNode: Object3D
    var model: Object3D?
}

class GroupHandler<G : ObjectGroup, M : IdentifiableWithModel>(
    private val scene: Scene,
    private val factory: (M) -> G,
    private val update: G.(M) -> Unit
) {

    val nodes = mutableMapOf<ObjectId, G>()
    private val messageCache = mutableMapOf<ObjectId, M>()
    private val holder = Object3D().also { scene += it }

    fun update(messages: List<M>) {
        val oldIds = nodes.keys.filter { true }
        addNew(messages)
        removeOld(messages, oldIds)
        updateRemaining(messages)
    }

    private fun addNew(messages: List<M>) {
        messages.filter { message ->
            !nodes.containsKey(message.id)
        }.forEach { message ->
            factory(message).also { node ->
                nodes[message.id] = node
                messageCache[message.id] = message
                holder.add(node)
            }
        }
    }

    private fun removeOld(
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
            messageCache.remove(id)
        }
    }

    private fun updateRemaining(messages: List<M>) {
        messages.forEach { message ->
            nodes[message.id]?.apply {
                this.update(message)
            }
            messageCache[message.id] = message
        }
    }

    fun assignModel(
        name: String,
        group: Group
    ) {
        messageCache.filter {
            it.value.model == name
        }.map {
            it.key
        }.forEach {
            nodes[it]?.let { node ->
                node.model = group.clone(true)
            }
        }
    }
}

fun Object3D.add(group: ObjectGroup) = add(group.rootNode)

fun Object3D.remove(group: ObjectGroup) = remove(group.rootNode)
