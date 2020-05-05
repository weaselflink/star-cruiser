package de.bissell.starcruiser.serializers

import kotlinx.serialization.*
import java.math.BigDecimal

@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer :
    KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor(
            "de.bissell.starcruiser.serializers.BigDecimalSerializer",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}
