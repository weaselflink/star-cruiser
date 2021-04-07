package de.stefanbissell.starcruiser

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.until
import kotlin.time.Duration

class GameTime(
    initialTime: Instant? = null
) {

    private var lastUpdate: Instant? = initialTime

    var current: Double = 0.0
        private set

    var delta: Double = 0.001
        private set

    var paused: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                lastUpdate = null
            }
        }

    fun update(now: Instant) {
        delta = lastUpdate?.let {
            (it.until(now, DateTimeUnit.MILLISECOND)) / 1_000.0
        } ?: 0.001
        current += delta
        lastUpdate = now
    }

    fun update(secondsToAdd: Number) {
        delta = secondsToAdd.toDouble()
        current += delta
        lastUpdate = lastUpdate?.plus(Duration.seconds(secondsToAdd.toDouble()))
    }

    companion object {
        fun atEpoch() = GameTime(Instant.EPOCH)
    }
}

fun Instant.until(other: Instant, unit: DateTimeUnit.TimeBased): Long =
    until(other, unit, TimeZone.UTC)

val Instant.Companion.EPOCH: Instant
    get() = fromEpochMilliseconds(0)
