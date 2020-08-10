package de.stefanbissell.starcruiser.minigames

class CircuitPathGame(
    val start: Pair<Int, Int>,
    val end: Pair<Int, Int>
) {

    val tiles = mutableListOf<Tile>()
    val isSolved: Boolean
        get() {
            val marked = mutableListOf(Tile(start.first, start.second, TileType.FULL))
            val unmarked = mutableListOf(Tile(end.first, end.second, TileType.FULL)).also {
                it.addAll(tiles)
            }
            var newlyMarked = unmarked.filter { candidate ->
                marked.any { it.connectsTo(candidate) }
            }
            while (newlyMarked.isNotEmpty()) {
                unmarked.removeAll(newlyMarked)
                marked.addAll(newlyMarked)

                newlyMarked = unmarked.filter { candidate ->
                    marked.any { it.connectsTo(candidate) }
                }
            }
            return marked.contains(Tile(end.first, end.second, TileType.FULL))
        }

    data class Tile(
        val column: Int,
        val row: Int,
        val type: TileType,
        var rotation: Int = 0
    ) {

        val connections: List<Int>
            get() = type.connections.map { it.rotate(rotation) }

        fun rotate() {
            rotation = rotation.rotate(1)
        }

        fun connectsTo(other: Tile) =
            connectsTo(other.column to other.row) &&
                other.connectsTo(column to row)

        private fun connectsTo(other: Pair<Int, Int>) =
            connectionTiles.contains(other.first to other.second)

        private val connectionTiles
            get() = connections.map {
                when (it) {
                    0 -> column to row - 1
                    1 -> column + 1 to row
                    2 -> column to row + 1
                    else -> column - 1 to row
                }
            }
    }

    enum class TileType(
        vararg connectedSides: Int
    ) {
        L(0, 1),
        I(0, 2),
        FULL(0, 1, 2, 3);

        val connections = connectedSides.toList()
    }
}

private fun Int.rotate(amount: Int) = (this + amount) % 4
