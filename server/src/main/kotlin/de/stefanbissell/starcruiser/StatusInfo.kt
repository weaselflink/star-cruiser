package de.stefanbissell.starcruiser

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class StatusInfo(
    val status: String,
    @Serializable(with = InstantSerializer::class)
    val upSince: Instant
)

class InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            serialName = "de.stefanbissell.starcruiser.InstantSerializer",
            kind = PrimitiveKind.STRING
        )

    override fun deserialize(decoder: Decoder): Instant =
        Instant.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}

fun Route.statusRoute() {

    val upSince = Clock.System.now()

    get("/status") {
        call.respond(
            StatusInfo(
                status = "up",
                upSince = upSince
            )
        )
    }
}
