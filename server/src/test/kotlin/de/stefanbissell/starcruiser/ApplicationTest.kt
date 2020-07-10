package de.stefanbissell.starcruiser

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.startsWith

class ApplicationTest {

    @Test
    fun testRoot() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/js/client.js").apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.OK)
                expectThat(response.content).isNotNull().startsWith("!function")
            }
        }
    }
}
