package de.stefanbissell.starcruiser.minigames

import de.stefanbissell.starcruiser.minigames.CircuitPathGame.Tile
import de.stefanbissell.starcruiser.minigames.CircuitPathGame.TileType
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class CircuitPathGameTest {

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
        expectThat(anLTile(3).connections).containsExactly(3, 0)
    }

    @Test
    fun `rotates I tile connections`() {
        expectThat(anITile(0).connections).containsExactly(0, 2)
        expectThat(anITile(1).connections).containsExactly(1, 3)
        expectThat(anITile(2).connections).containsExactly(2, 0)
        expectThat(anITile(3).connections).containsExactly(3, 1)
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
        expectThat(aGame("XL0X").isSolved).isFalse()
        expectThat(aGame("XI1X").isSolved).isTrue()
        expectThat(aGame("XI1,I3X").isSolved).isTrue()
        expectThat(aGame("XI1,I2X").isSolved).isFalse()
        expectThat(aGame("XI1,L2,L0;I0,L0,I1X").isSolved).isTrue()
        expectThat(aGame("XI1,L2,L0;I0,L1,I1X").isSolved).isFalse()
        expectThat(aGame("XL2,L1X;L0,L3").isSolved).isTrue()
        expectThat(aGame("XL2,L1X;L0,I3").isSolved).isFalse()
        expectThat(aGame("L1,I1,L2;L0,L2,L0X;XI1,L3,I0").isSolved).isTrue()
        expectThat(aGame("L1,I1,L2;L0,L2,L2X;XI1,L3,I0").isSolved).isFalse()
    }

    @Test
    fun `can create random solved`() {
        repeat(10) {
            expectThat(
                CircuitPathGame.createSolved().isSolved
            ).isTrue()
        }
    }

    private fun aGame(setup: String): CircuitPathGame {
        val rows = setup.split(";")
        val columnCount = rows.first().count { it == ',' } + 1
        val game = CircuitPathGame(
            start = -1 to rows.indexOfFirst { it.startsWith("X") },
            end = columnCount to rows.indexOfFirst { it.endsWith("X") }
        )
        rows.map {
            it.removePrefix("X").removeSuffix("X")
        }.forEachIndexed { rowIndex, row ->
            row.split(",").forEachIndexed { columnIndex, column ->
                game.tiles += Tile(
                    column = columnIndex,
                    row = rowIndex,
                    type = if (column.startsWith("L")) TileType.L else TileType.I,
                    rotation = column.substring(1).toInt()
                )
            }
        }
        return game
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
