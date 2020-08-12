package de.stefanbissell.starcruiser.minigames

import de.stefanbissell.starcruiser.minigames.CircuitPathGame.Tile
import de.stefanbissell.starcruiser.minigames.CircuitPathGame.TileType
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isTrue

class CircuitPathGameTest {

    @Test
    fun `creates path`() {
        val width = 10
        repeat(5) {
            val path = PathFinder(width, 4).path
            expectThat(path.first().column).isEqualTo(-1)
            expectThat(path.last().column).isEqualTo(width)
            expectThat(path.size).isGreaterThanOrEqualTo(width + 2)
            path.windowed(size = 2).forEach { window ->
                expectThat(window[0].isNextTo(window[1]))
            }
        }
    }

    @Test
    fun `rotates tile`() {
        expectThat(anLTile(0).apply { rotate() }).isEqualTo(anLTile(1))
        expectThat(anLTile(1).apply { rotate() }).isEqualTo(anLTile(2))
        expectThat(anLTile(2).apply { rotate() }).isEqualTo(anLTile(3))
        expectThat(anLTile(3).apply { rotate() }).isEqualTo(anLTile(0))
    }

    @Test
    fun `rotates L tile connections`() {
        expectThat(anLTile(0).connections).containsExactly(0, 1)
        expectThat(anLTile(1).connections).containsExactly(1, 2)
        expectThat(anLTile(2).connections).containsExactly(2, 3)
        expectThat(anLTile(3).connections).containsExactly(0, 3)
    }

    @Test
    fun `rotates I tile connections`() {
        expectThat(anITile(0).connections).containsExactly(0, 2)
        expectThat(anITile(1).connections).containsExactly(1, 3)
        expectThat(anITile(2).connections).containsExactly(0, 2)
        expectThat(anITile(3).connections).containsExactly(1, 3)
    }

    @Test
    fun `checks if tiles are connected`() {
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(1, 0, TileType.L, 0))
        ).isFalse()
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(1, 0, TileType.L, 2))
        ).isTrue()
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(1, 0, TileType.L, 3))
        ).isTrue()
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(2, 0, TileType.L, 2))
        ).isFalse()
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(1, 0, TileType.I, 0))
        ).isFalse()
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(1, 0, TileType.I, 1))
        ).isTrue()
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(1, 0, TileType.I, 2))
        ).isFalse()
        expectThat(
            Tile(0, 0, TileType.L, 0)
                .connectsTo(Tile(1, 0, TileType.I, 3))
        ).isTrue()
        expectThat(
            Tile(0, 0, TileType.I, 0)
                .connectsTo(Tile(0, 1, TileType.I, 0))
        ).isTrue()
        expectThat(
            Tile(0, 1, TileType.I, 0)
                .connectsTo(Tile(0, 0, TileType.I, 0))
        ).isTrue()
    }

    @Test
    fun `detects valid solution`() {
        expectThat(aGame("X─X").isSolved).isTrue()
        expectThat(aGame("X└X").isSolved).isFalse()
        expectThat(aGame("X──X").isSolved).isTrue()
        expectThat(aGame("X─│X").isSolved).isFalse()
        expectThat(
            aGame(
                """
                X─┐└
                 │└─X
                """
            ).isSolved
        ).isTrue()
        expectThat(
            aGame(
                """
                X─┐└
                 │──X
                """
            ).isSolved
        ).isFalse()
        expectThat(
            aGame(
                """
                X───
                 │└─X
                """
            ).isSolved
        ).isFalse()
        expectThat(
            aGame(
                """
                X┐┌X
                 └┘
                """
            ).isSolved
        ).isTrue()
        expectThat(
            aGame(
                """
                X┐┌X
                 └│
                """
            ).isSolved
        ).isFalse()
        expectThat(
            aGame(
                """
                 ┌─┐
                 └┐└X
                X─┘│
                """
            ).isSolved
        ).isTrue()
        expectThat(
            aGame(
                """
                 ┌─┐
                 └┐┐X
                X─┘│
                """
            ).isSolved
        ).isFalse()
    }

    @Test
    fun `can solve game by rotating tiles`() {
        val game = aGame(
            """
                 ┌─┐
                 └┐┐X
                X│┘│
                """
        )

        game.rotateTile(0, 2)
        game.rotateTile(2, 1)

        expectThat(
            game.isSolved
        ).isFalse()

        game.rotateTile(2, 1)

        expectThat(
            game.isSolved
        ).isTrue()
    }

    @Test
    fun `encodes tiles to string`() {
        val game = aGame(
            """
                 ┌─┐
                 └┐┐X
                X│┘│
                """
        )

        expectThat(
            game.encodedTiles
        ).isEqualTo("12,13,23;01,23,23;02,03,02")
    }

    @Test
    fun `can create random solved`() {
        repeat(5) {
            val game = CircuitPathGame.createSolved(10, 4)
            expectThat(
                game.isSolved
            ).isTrue()
        }
    }

    @Test
    fun `can create random unsolved`() {
        repeat(5) {
            val game = CircuitPathGame.createUnsolved(10, 4)
            expectThat(
                game.isSolved
            ).isFalse()
        }
    }

    private fun aGame(setup: String): CircuitPathGame {
        val rows = setup.trim().trim('\n').split("\n").map { it.trim() }
        val columnCount = rows.first().trim('X').length
        val game = CircuitPathGame(
            width = columnCount,
            height = rows.size,
            start = -1 to rows.indexOfFirst { it.startsWith("X") },
            end = columnCount to rows.indexOfFirst { it.endsWith("X") }
        )
        rows.map {
            it.trim('X')
        }.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, column ->
                game.tiles += column.parseTile(columnIndex, rowIndex)
            }
        }
        return game
    }

    private fun Char.parseTile(column: Int, row: Int) =
        when (this) {
            '│' -> Tile(column, row, TileType.I, 0)
            '─' -> Tile(column, row, TileType.I, 1)
            '└' -> Tile(column, row, TileType.L, 0)
            '┌' -> Tile(column, row, TileType.L, 1)
            '┐' -> Tile(column, row, TileType.L, 2)
            else -> Tile(column, row, TileType.L, 3)
        }

    private fun anLTile(rotation: Int) = Tile(
        column = 0,
        row = 0,
        type = TileType.L,
        rotation = rotation
    )

    private fun anITile(rotation: Int) = Tile(
        column = 0,
        row = 0,
        type = TileType.I,
        rotation = rotation
    )
}
