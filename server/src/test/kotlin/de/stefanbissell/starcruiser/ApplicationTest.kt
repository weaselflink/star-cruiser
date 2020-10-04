package de.stefanbissell.starcruiser

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.startsWith

class ApplicationTest {

    @Test
    fun `provides static resources`() {
        testRequest(HttpMethod.Get, "/js/client.js") {
            expectThat(response.status())
                .isEqualTo(HttpStatusCode.OK)
            expectThat(response.content)
                .isNotNull()
                .startsWith("!function")
        }
    }

    @Test
    fun `redirects after restart`() {
        testRequest(HttpMethod.Get, "/restart") {
            expectThat(response.status())
                .isEqualTo(HttpStatusCode.Found)
            expectThat(response.headers["location"])
                .isNotNull()
                .isEqualTo("/")
        }
    }

    private fun testRequest(
        method: HttpMethod,
        uri: String,
        block: TestApplicationCall.() -> Unit
    ) {
        withTestApplication({ module() }) {
            handleRequest(method, uri).apply(block)
        }
    }
}
