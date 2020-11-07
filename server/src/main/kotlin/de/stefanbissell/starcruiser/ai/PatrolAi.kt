package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.Vector2

class PatrolAi(
    private val behaviourAi: BehaviourAi,
    private val helmAi: HelmAi
) : ComponentAi(2.0) {

    var currentPatrolOrder: Order.Patrol? = null
    var pointInPath: Int = 0

    private val path
        get() = currentPatrolOrder?.path ?: emptyList()
    private val nextPoint: Vector2
        get() = path[pointInPath]

    override fun execute(aiState: AiState) {
        aiState.updatePatrol()
    }

    private fun AiState.updatePatrol() {
        if (currentPatrolOrder != patrolOrder) {
            currentPatrolOrder = patrolOrder
            pointInPath = 0
        }
        if (currentPatrolOrder != null && behaviourAi.behaviour is Behaviour.Patrol) {
            performPatrol()
        }
    }

    private fun AiState.performPatrol() {
        if (path.isNotEmpty()) {
            checkPointReached()
            steerTowardsNextPoint()
        } else {
            ship.throttle = 0
        }
    }

    private fun AiState.checkPointReached() {
        if (nextPointRange < 50) {
            incrementNextPoint()
        }
    }

    private fun incrementNextPoint() {
        pointInPath = (pointInPath + 1) % path.size
    }

    private fun AiState.steerTowardsNextPoint() {
        ship.throttle = 50
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = nextPointAngle
        }
    }

    private val AiState.nextPointRelative
        get() = nextPoint - ship.position

    private val AiState.nextPointRange
        get() = nextPointRelative.length()

    private val AiState.nextPointAngle
        get() = nextPointRelative.angle()

    private val AiState.patrolOrder: Order.Patrol?
        get() = orders.filterIsInstance<Order.Patrol>().firstOrNull()
}
