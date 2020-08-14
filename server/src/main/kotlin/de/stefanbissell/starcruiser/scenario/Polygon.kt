package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Vector2

data class Polygon(
    val border: List<Vector2>
) : Area {

    private val edges: List<Pair<Vector2, Vector2>> =
        border.mapIndexed { index, point ->
            if (index < border.size - 1) {
                point to border[index + 1]
            } else {
                point to border[0]
            }
        }

    init {
        if (border.size < 3) throw IllegalArgumentException("Need at least 3 points.")
    }

    override val boundingBox: Box =
        Box(
            Vector2(border.map { it.x }.minOrNull()!!, border.map { it.y }.minOrNull()!!),
            Vector2(border.map { it.x }.maxOrNull()!!, border.map { it.y }.maxOrNull()!!)
        )

    override fun isInside(point: Vector2): Boolean {
        return edges.count {
            intersects(point, it)
        } % 2 == 1
    }

    private fun intersects(point: Vector2, edge: Pair<Vector2, Vector2>) =
        intersectsOnYAxis(edge, point) && intersectsLeftOfPoint(point, edge)

    private fun intersectsOnYAxis(edge: Pair<Vector2, Vector2>, point: Vector2) =
        (edge.first.y > point.y) != (edge.second.y > point.y)

    private fun intersectsLeftOfPoint(point: Vector2, edge: Pair<Vector2, Vector2>): Boolean {
        return (
            point.x < (
                (edge.second.x - edge.first.x) *
                    (point.y - edge.first.y) /
                    (edge.second.y - edge.first.y) +
                    edge.first.x
                )
            )
    }

    companion object {
        fun of(vararg points: Vector2) = Polygon(points.toList())
    }
}
