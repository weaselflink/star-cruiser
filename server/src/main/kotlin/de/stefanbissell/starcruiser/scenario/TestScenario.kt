package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.p

object TestScenario : Scenario() {

    override val definition =
        scenario {
            asteroidField {
                density = 25.0
                area(
                    p(0, 0),
                    p(0, 500),
                    p(1000, 500),
                    p(1000, 0)
                )
            }
        }
}
