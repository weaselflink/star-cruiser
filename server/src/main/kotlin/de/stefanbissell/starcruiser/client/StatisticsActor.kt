package de.stefanbissell.starcruiser.client

import de.stefanbissell.starcruiser.client.StatisticsMessage.GetSnapshot
import de.stefanbissell.starcruiser.client.StatisticsMessage.MessageReceived
import de.stefanbissell.starcruiser.client.StatisticsMessage.MessageSent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class StatisticsMessage {

    class MessageSent(val size: Int) : StatisticsMessage()
    class MessageReceived(val size: Int) : StatisticsMessage()
    class GetSnapshot(val response: CompletableDeferred<StatisticsSnapshot>) : StatisticsMessage()
}

fun CoroutineScope.createStatisticsActor() = actor<StatisticsMessage> {
    var countSent = 0L
    var totalSent = 0L
    var countReceived = 0L
    var totalReceived = 0L

    launch {
        while (true) {
            delay(10_000)
            val response = CompletableDeferred<StatisticsSnapshot>()
            channel.send(GetSnapshot(response))
            println(response.await())
        }
    }

    for (message in channel) {
        when (message) {
            is MessageSent -> {
                countSent++
                totalSent += message.size
            }
            is MessageReceived -> {
                countReceived++
                totalReceived += message.size
            }
            is GetSnapshot -> message.response.complete(
                StatisticsSnapshot(countSent, totalSent, countReceived, totalReceived)
            )
        }
    }
}

data class StatisticsSnapshot(
    val countSent: Long,
    val totalSent: Long,
    val countReceived: Long,
    val totalReceived: Long
)
