package de.stefanbissell.starcruiser.client

import de.stefanbissell.starcruiser.client.StatisticsMessage.GetSnapshot
import de.stefanbissell.starcruiser.client.StatisticsMessage.MessageReceived
import de.stefanbissell.starcruiser.client.StatisticsMessage.MessageSent
import de.stefanbissell.starcruiser.configuredJson
import de.stefanbissell.starcruiser.round
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private const val statisticsLogIntervalSeconds = 10

sealed class StatisticsMessage {

    class MessageSent(val size: Int) : StatisticsMessage()
    class MessageReceived(val size: Int) : StatisticsMessage()
    class GetSnapshot(val response: CompletableDeferred<StatisticsSnapshot>) : StatisticsMessage()
}

fun CoroutineScope.statisticsActor(): SendChannel<StatisticsMessage> =
    Channel<StatisticsMessage>().also { channel ->
        startLogger(channel)
        startConsumer(channel)
    }

private fun CoroutineScope.startLogger(channel: Channel<StatisticsMessage>) {
    launch {
        while (true) {
            delay(statisticsLogIntervalSeconds.seconds)
            val response = CompletableDeferred<StatisticsSnapshot>()
            channel.send(GetSnapshot(response))
            response.await().log()
        }
    }
}

private fun CoroutineScope.startConsumer(channel: Channel<StatisticsMessage>) {
    launch {
        var countSent = 0L
        var bytesSent = 0L
        var totalCountSent = 0L
        var totalBytesSent = 0L
        var countReceived = 0L
        var bytesReceived = 0L
        var totalCountReceived = 0L
        var totalBytesReceived = 0L

        for (message in channel) {
            when (message) {
                is MessageSent -> {
                    countSent++
                    bytesSent += message.size
                }
                is MessageReceived -> {
                    countReceived++
                    bytesReceived += message.size
                }
                is GetSnapshot -> {
                    totalCountSent += countSent
                    totalBytesSent += bytesSent
                    totalCountReceived += countReceived
                    totalBytesReceived += bytesReceived
                    message.response.complete(
                        StatisticsSnapshot(
                            countSent = countSent,
                            bytesSent = bytesSent,
                            totalCountSent = totalCountSent,
                            totalBytesSent = totalBytesSent,
                            countReceived = countReceived,
                            bytesReceived = bytesReceived,
                            totalCountReceived = totalCountReceived,
                            totalBytesReceived = totalBytesReceived
                        )
                    )

                    countSent = 0
                    bytesSent = 0
                    countReceived = 0
                    bytesReceived = 0
                }
            }
        }
    }
}

@Serializable
data class StatisticsSnapshot(
    var countSent: Long,
    var bytesSent: Long,
    var totalCountSent: Long,
    var totalBytesSent: Long,
    var countReceived: Long,
    var bytesReceived: Long,
    var totalCountReceived: Long,
    var totalBytesReceived: Long
) {

    private val nowWithoutNanos: Instant
        get() = Clock.System.now().let {
            it.minus(it.nanosecondsOfSecond.nanoseconds)
        }

    fun toJson() = configuredJson.encodeToString(serializer(), this)

    fun log() {
        println("============== $nowWithoutNanos ==============")
        println(
            "Messages sent:     " +
                "${totalCountSent.toString().padStart(12)} " +
                "(${(countSent / statisticsLogIntervalSeconds).toString().padStart(12)}/s)"
        )
        println(
            "Bytes sent:        " +
                "${totalBytesSent.formatBytes().padStart(12)} " +
                "(${(bytesSent / statisticsLogIntervalSeconds).formatBytes().padStart(12)}/s)"
        )
        println(
            "Messages received: " +
                "${totalCountReceived.toString().padStart(12)} " +
                "(${(countReceived / statisticsLogIntervalSeconds).toString().padStart(12)}/s)"
        )
        println(
            "Bytes received:    " +
                "${totalBytesReceived.formatBytes().padStart(12)} " +
                "(${(bytesReceived / statisticsLogIntervalSeconds).formatBytes().padStart(12)}/s)"
        )
    }

    private fun Long.formatBytes(): String {
        scales.forEach {
            if (this / it.first >= 1) {
                return "${(this / it.first.toDouble()).round(2)} ${it.second}"
            }
        }
        return "0 B"
    }

    companion object {
        private val scales = listOf(
            1024L * 1024L * 1024L to "GB",
            1024L * 1024L to "MB",
            1024L to "kB",
            1L to "B"
        )
    }
}
