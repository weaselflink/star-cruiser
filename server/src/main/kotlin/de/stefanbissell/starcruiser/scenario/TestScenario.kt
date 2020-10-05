package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.ships.Faction

object TestScenario : Scenario() {

    override val definition =
        scenario {
            repeat(3) {
                nonPlayerShip {
                    faction = Faction.Enemy
                }
            }
            repeat(3) {
                nonPlayerShip {
                    faction = Faction.Neutral
                }
            }
            asteroidField {
                density = 0.5
                area(
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
                area(
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
        }
}
