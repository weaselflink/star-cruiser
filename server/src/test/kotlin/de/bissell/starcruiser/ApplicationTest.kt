package de.bissell.starcruiser

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.ObsoleteCoroutinesApi
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.startsWith
import kotlin.test.Test

@ObsoleteCoroutinesApi
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
