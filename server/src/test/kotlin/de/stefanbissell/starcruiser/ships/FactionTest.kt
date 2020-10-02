package de.stefanbissell.starcruiser.ships

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class FactionTest {

    @Test
    fun `hostile to faction in enemies`() {
        expectThat(Faction.Player isHostileTo Faction.Enemy).isTrue()
        expectThat(Faction.Enemy isHostileTo Faction.Player).isTrue()
    }

    @Test
    fun `is not hostile to faction not in enemies`() {
        expectThat(Faction.Player isHostileTo Faction.Player).isFalse()
        expectThat(Faction.Player isHostileTo Faction.Neutral).isFalse()
        expectThat(Faction.Enemy isHostileTo Faction.Enemy).isFalse()
        expectThat(Faction.Enemy isHostileTo Faction.Neutral).isFalse()
        expectThat(Faction.Neutral isHostileTo Faction.Neutral).isFalse()
        expectThat(Faction.Neutral isHostileTo Faction.Player).isFalse()
        expectThat(Faction.Neutral isHostileTo Faction.Enemy).isFalse()
    }
}
