package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.shapes.Circle
import de.stefanbissell.starcruiser.shapes.Polygon
import de.stefanbissell.starcruiser.shapes.Ring

object TestScenario : Scenario() {

    private val defaultSpawnArea = Ring(
        center = p(0, 0),
        outer = 1400.0,
        inner = 800.0
    )

    override val definition =
        scenario {
            playerSpawnArea = Circle(p(0, 0), 300)
            factions {
                faction {
                    name = "Enarian"
                    enemies = listOf("Reynor")
                    forPlayers = true
                }
                faction {
                    name = "Janis"
                }
                faction {
                    name = "Reynor"
                    enemies = listOf("Enarian")
                }
            }
            repeat(3) {
                nonPlayerShip {
                    faction = "Reynor"
                    spawnArea = defaultSpawnArea
                }
            }
            repeat(3) {
                nonPlayerShip {
                    faction = "Janis"
                    spawnArea = defaultSpawnArea
                }
            }
            asteroidField {
                density = 0.5
                shape = Polygon(
                    p(-300, -300),
                    p(200, -300),
                    p(500, 0),
                    p(500, 300),
                    p(600, 400),
                    p(800, 400),
                    p(900, 300),
                    p(900, -100),
                    p(300, -700),
                    p(-300, -700),
                    p(-400, -600),
                    p(-400, -400)
                )
            }
            asteroidField {
                density = 0.5
                shape = Polygon(
                    p(-1300, 500),
                    p(-1300, 300),
                    p(-1200, 200),
                    p(-1000, 200),
                    p(-900, 100),
                    p(-900, -100),
                    p(-1100, -300),
                    p(-1600, -300),
                    p(-1800, -100),
                    p(-1800, 400),
                    p(-1600, 600),
                    p(-1400, 600)
                )
            }
            asteroidField {
                density = 0.5
                shape = Circle(
                    center = p(0, 2000),
                    radius = 500
                )
            }
        }
}
