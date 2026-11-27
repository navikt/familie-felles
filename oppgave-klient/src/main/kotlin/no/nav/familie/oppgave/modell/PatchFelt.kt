package no.nav.familie.oppgave.modell

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.StdSerializer

/**
 * Skiller mellom utelatt felt, eksplisitt null og en konkret verdi.
 * Feltinkluderingen på PatchOppgaveRequest utelater bare Utelatt, ikke Null eller tomme verdier.
 */
@JsonSerialize(using = PatchFeltSerializer::class)
sealed class PatchFelt<out T> {
    data object Utelatt : PatchFelt<Nothing>()

    data object Null : PatchFelt<Nothing>()

    data class Verdi<T : Any>(
        val verdi: T,
    ) : PatchFelt<T>()
}

class PatchFeltSerializer : StdSerializer<PatchFelt<*>>(PatchFelt::class.java) {
    override fun isEmpty(
        ctxt: SerializationContext,
        value: PatchFelt<*>,
    ): Boolean = value === PatchFelt.Utelatt

    override fun serialize(
        value: PatchFelt<*>,
        gen: JsonGenerator,
        ctxt: SerializationContext,
    ) {
        when (value) {
            PatchFelt.Utelatt -> error("Utelatt patchfelt skal filtreres bort før serialisering")
            PatchFelt.Null -> gen.writeNull()
            is PatchFelt.Verdi -> ctxt.findValueSerializer(value.verdi.javaClass).serialize(value.verdi, gen, ctxt)
        }
    }
}
