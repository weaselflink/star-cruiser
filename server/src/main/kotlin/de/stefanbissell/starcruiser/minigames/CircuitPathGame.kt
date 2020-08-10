package de.stefanbissell.starcruiser.minigames

import kotlin.random.Random

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

    fun fillUpTiles() {
        val width = tiles.maxBy { it.column }!!.column + 1
        val height = tiles.maxBy { it.row }!!.row + 1
        (0..width).forEach { column ->
            (0..height).forEach { row ->
                if (tiles.none { it.column == column && it.row == row }) {
                    tiles += Tile.random(column, row)
                }
            }
        }
    }

    fun randomizeTiles() {
        tiles.forEach {
            it.rotation = Random.nextInt(4)
        }
    }

    companion object {
        fun createSolved(): CircuitPathGame {
            val path = createPath()
            val game = CircuitPathGame(
                start = path.first(),
                end = path.last()
            )
            path.windowed(size = 3, step = 1).forEach { window ->
                val pos = window[1]
                val tile = when {
                    window[0].second == window[2].second ->
                        Tile(pos.first, pos.second, TileType.I, 1)
                    window[0].second < window[2].second && window[0].second == window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 2)
                    window[0].second > window[2].second && window[0].second == window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 3)
                    window[0].second < window[2].second && window[0].second != window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 0)
                    else ->
                        Tile(pos.first, pos.second, TileType.L, 1)
                }
                game.tiles += tile
            }
            game.fillUpTiles()
            return game
        }

        private fun createPath(): MutableList<Pair<Int, Int>> {
            var column = 0
            var row = Random.nextInt(2)
            val path = mutableListOf(column to row)
            while (column < 7) {
                if (path.count { it.first == column } > 1 || Random.nextBoolean()) {
                    column++
                    path += column to row
                } else {
                    row = (row + 1) % 2
                    path += column to row
                }
            }
            path.add(0, -1 to path.first().second)
            path.add(8 to path.last().second)
            return path
        }
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

        companion object {
            fun random(column: Int, row: Int): Tile {
                val type = if (Random.nextBoolean()) {
                    TileType.I
                } else {
                    TileType.L
                }
                return Tile(column, row, type, Random.nextInt(4))
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
