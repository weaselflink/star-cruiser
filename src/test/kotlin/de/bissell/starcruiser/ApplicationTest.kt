package de.bissell.starcruiser

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.serialization.UnstableDefault
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.startsWith

@UnstableDefault
class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/static/bla.js").apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.OK)
                expectThat(response.content).isNotNull().startsWith("var wsBaseUri")
            }
        }
    }
}
