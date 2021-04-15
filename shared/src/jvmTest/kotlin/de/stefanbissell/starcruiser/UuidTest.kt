package de.stefanbissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isNotEqualTo
import strikt.assertions.matches

class UuidTest {

    @Test
    fun `creates UUID in correct format`() {
        expectThat(Uuid().toString())
            .matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
    }

    @Test
    fun `creates distinct UUIDs`() {
        expectThat(Uuid().toString())
            .isNotEqualTo(Uuid().toString())
    }
}
