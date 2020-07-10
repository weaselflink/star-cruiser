package de.stefanbissell.starcruiser.client

import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ThrottleActorTest {

    @Test
    fun `records inflight messages`() {
        runActorTest {
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))

            val result = CompletableDeferred<Int>()
            send(ThrottleMessage.GetInflightMessageCount(result))

            expectThat(result.await()).isEqualTo(3)
        }
    }

    @Test
    fun `uses acknowledgement messages`() {
        runActorTest {
            val firstResult = CompletableDeferred<Long>()
            send(ThrottleMessage.AddInflightMessage(firstResult))
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))

            send(ThrottleMessage.AcknowledgeInflightMessage(firstResult.await()))

            val result = CompletableDeferred<Int>()
            send(ThrottleMessage.GetInflightMessageCount(result))

            expectThat(result.await()).isEqualTo(2)
        }
    }

    @Test
    fun `ignores acknowledgement messages with wrong id`() {
        runActorTest {
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))
            send(ThrottleMessage.AddInflightMessage(CompletableDeferred()))

            send(ThrottleMessage.AcknowledgeInflightMessage(Random.nextLong()))

            val result = CompletableDeferred<Int>()
            send(ThrottleMessage.GetInflightMessageCount(result))

            expectThat(result.await()).isEqualTo(3)
        }
    }

    private fun runActorTest(block: suspend SendChannel<ThrottleMessage>.() -> Unit) {
        runBlocking {
            val channel = createThrottleActor()
            try {
                channel.block()
            } finally {
                channel.close()
            }
        }
    }
}
