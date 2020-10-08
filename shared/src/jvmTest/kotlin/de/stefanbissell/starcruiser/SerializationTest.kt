package de.stefanbissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SerializationTest {

    @Test
    fun `serializes game state`() {
        expectThat(gameState.toJson())
            .isEqualTo(serializedGameState)
    }

    @Test
    fun `deserializes game state`() {
        expectThat(GameStateMessage.parse(serializedGameState))
            .isEqualTo(gameState)
    }

    private val gameState = GameStateMessage(
        counter = 42L,
        snapshot = SnapshotMessage.ShipSelection(
            playerShips = listOf(
                PlayerShipMessage(
                    id = ObjectId("ship1"),
                    name = "Serenity",
                    shipClass = "Firefly"
                ),
                PlayerShipMessage(
                    id = ObjectId("ship2"),
                    name = "Nostromo",
                    shipClass = null
                )
            )
        )
    )

    private val serializedGameState =
        """
            {
                "counter": 42,
                "snapshot": {
                    "type": "de.stefanbissell.starcruiser.SnapshotMessage.ShipSelection",
                    "playerShips": [
                        {
                            "id": "ship1",
                            "name": "Serenity",
                            "shipClass": "Firefly"
                        },
                        {
                            "id": "ship2",
                            "name": "Nostromo",
                            "shipClass": null
                        }
                    ]
                }
            }
        """.trimIndent()
}
