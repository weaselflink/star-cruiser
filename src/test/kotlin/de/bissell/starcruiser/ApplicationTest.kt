package de.bissell.starcruiser

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.startsWith

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/static/bla.js").apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.OK)
                expectThat(response.content).isNotNull().startsWith("var wsBaseUri")
            }
        }
    }
}
