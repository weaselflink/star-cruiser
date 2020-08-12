package de.stefanbissell.starcruiser.minigames

import kotlin.random.Random

class CircuitPathGame(
    val width: Int,
    val height: Int,
    val start: Pair<Int, Int>,
    val end: Pair<Int, Int>
) {

    val tiles = mutableListOf<Tile>()
    val isSolved: Boolean
        get() = markReachedTiles().contains(Tile(end.first, end.second, TileType.FULL))
    val encodedTiles: String
        get() {
            val reached = markReachedTiles()

            return (0 until height).joinToString(";") { row ->
                tiles.filter { it.row == row }
                    .sortedBy { it.column }.joinToString(",") { tile ->
                        if (reached.any { it.column == tile.column && it.row == tile.row }) {
                            "+" + tile.connections.joinToString("")
                        } else {
                            tile.connections.joinToString("")
                        }
                    }
            }
        }

    fun rotateTile(column: Int, row: Int) {
        tiles.firstOrNull { it.column == column && it.row == row }
            ?.apply { rotate() }
    }

    private fun fillUpTiles() {
        (0 until width).forEach { column ->
            (0 until height).forEach { row ->
                if (tiles.none { it.column == column && it.row == row }) {
                    tiles += Tile.random(column, row)
                }
            }
        }
    }

    private fun randomizeTiles() {
        tiles.forEach {
            it.rotation = Random.nextInt(4)
        }
    }

    private fun markReachedTiles(): MutableList<Tile> {
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
        return marked
    }

    companion object {
        fun createSolved(width: Int, height: Int): CircuitPathGame {
            val path = PathFinder(width, height).path.map { it.column to it.row }
            val game = CircuitPathGame(
                width = width,
                height = height,
                start = path.first(),
                end = path.last()
            )
            path.windowed(size = 3).forEach { window ->
                val pos = window[1]
                val columnChange = window[2].first - window[0].first
                val rowChange = window[2].second - window[0].second
                val tile = when {
                    window[0].second == window[2].second ->
                        Tile(pos.first, pos.second, TileType.I, 1)
                    window[0].first == window[2].first ->
                        Tile(pos.first, pos.second, TileType.I, 0)

                    columnChange < 0 && rowChange < 0 && window[0].second == window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 0)
                    columnChange > 0 && rowChange > 0 && window[0].second != window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 0)

                    columnChange > 0 && rowChange < 0 && window[0].second != window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 1)
                    columnChange < 0 && rowChange > 0 && window[0].second == window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 1)

                    columnChange < 0 && rowChange < 0 && window[0].second != window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 2)
                    columnChange > 0 && rowChange > 0 && window[0].second == window[1].second ->
                        Tile(pos.first, pos.second, TileType.L, 2)

                    else ->
                        Tile(pos.first, pos.second, TileType.L, 3)
                }
                game.tiles += tile
            }
            game.fillUpTiles()
            return game
        }

        fun createUnsolved(width: Int, height: Int) =
            createSolved(width, height).apply {
                while (isSolved) {
                    randomizeTiles()
                }
            }
    }

    data class Tile(
        val column: Int,
        val row: Int,
        val type: TileType,
        var rotation: Int = 0
    ) {

        val connections: List<Int>
            get() = type.connections.map { it.rotate(rotation) }.sorted()

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

class PathFinder(
    private val width: Int,
    private val height: Int = 2,
    private val random: Random = Random
) {

    val path = mutableListOf<Step>()
    private val permutations = (0 until (width * height)).map { permutation() }
    private val decisions = (0 until (width * height)).map { 0 }.toMutableList()
    private var pos = 0

    init {
        findValidPath(
            start = Step(0, random.nextInt(height)),
            end = Step(width - 1, random.nextInt(height))
        )
        path.add(0, Step(-1, path.first().row))
        path.add(Step(width, path.last().row))
    }

    private fun findValidPath(start: Step, end: Step) {
        traversePath(start, end)
        while (!path.contains(end)) {
            incrementDecision()
            traversePath(start, end)
        }
    }

    private fun incrementDecision() {
        if (pos < 0) {
            throw Exception()
        }
        decisions[pos] += 1
        if (decisions[pos] >= validDecisions()) {
            decisions[pos] = 0
            path.removeAt(path.size - 1)
            pos--
            incrementDecision()
        }
    }

    private fun validDecisions() = (0..3).count { isValid(path.last().next(it)) }

    private fun traversePath(start: Step, end: Step) {
        path.clear()
        pos = 0
        path += start
        var next = continueOnPath()
        while (isValid(next) && !path.contains(end)) {
            path += next!!
            pos++
            next = continueOnPath()
        }
    }

    private fun continueOnPath(): Step? {
        val valid = permutations[pos].filter {
            isValid(path.last().next(it))
        }
        if (decisions[pos] >= valid.size) {
            return null
        }
        return path.last().next(valid[decisions[pos]])
    }

    private fun isValid(toCheck: Step?) =
        toCheck != null && !path.contains(toCheck) &&
            toCheck.column >= 0 && toCheck.column < width &&
            toCheck.row >= 0 && toCheck.row < height

    private fun permutation(): List<Int> {
        val initial = (0..3).toMutableList()
        val result = mutableListOf<Int>()
        while (initial.isNotEmpty()) {
            val num = initial.removeAt(random.nextInt(initial.size))
            result += num
        }
        return result.toList()
    }

    data class Step(
        val column: Int,
        val row: Int
    ) {

        fun isNextTo(other: Step) {
            (0..3).any { next(it) == other }
        }

        fun next(direction: Int) =
            when (direction) {
                0 -> Step(column, row - 1)
                1 -> Step(column + 1, row)
                2 -> Step(column, row + 1)
                else -> Step(column - 1, row)
            }
    }
}

private fun Int.rotate(amount: Int) = (this + amount) % 4
