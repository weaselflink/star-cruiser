package de.stefanbissell.starcruiser

import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

class ApplicationTest {

    @Test
    fun `provides static resources`() {
        testRequest("/js/client.js") { response ->
            expectThat(response.status)
                .isEqualTo(HttpStatusCode.OK)
            expectThat(response.bodyAsText())
                .startsWith("!function")
        }
    }

    @Test
    fun `redirects after restart`() {
        testRequest("/restart") { response ->
            expectThat(response.status)
                .isEqualTo(HttpStatusCode.OK)
            expectThat(response.bodyAsText())
                .startsWith("<!DOCTYPE html>")
        }
    }

    private fun testRequest(
        uri: String,
        block: suspend (HttpResponse) -> Unit
    ) = testApplication {
        application {
            module()
        }
        val response = client.get(uri)
        block(response)
    }
}
