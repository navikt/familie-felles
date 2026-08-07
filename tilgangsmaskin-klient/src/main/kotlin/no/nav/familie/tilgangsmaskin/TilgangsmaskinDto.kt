package no.nav.familie.tilgangsmaskin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory

enum class Regeltype {
    KJERNE_REGELTYPE,
    KOMPLETT_REGELTYPE,
    OVERSTYRBAR_REGELTYPE,
}

data class BrukerIdOgRegelsettDto(
    val brukerId: String,
    val type: Regeltype,
) {
    override fun toString(): String = "BrukerIdOgRegelsettDto(brukerId=${brukerId.maskerIdent()}, type=$type)"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TilgangsmaskinBulkResponsDto(
    val resultater: List<TilgangsmaskinBulkResultatDto>,
    val ansattId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TilgangsmaskinBulkResultatDto(
    val brukerId: String,
    val status: Int,
    val detaljer: TilgangsmaskinAvvisningDto? = null,
) {
    override fun toString(): String =
        "TilgangsmaskinBulkResultatDto(brukerId=${brukerId.maskerIdent()}, status=$status, detaljer=$detaljer)"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TilgangsmaskinAvvisningDto(
    val title: String?,
    val begrunnelse: String? = null,
    val traceId: String? = null,
    val kanOverstyres: Boolean = false,
) {
    fun avvisningskode(): Avvisningskode = Avvisningskode.fraTitle(title)
}

enum class Avvisningskode {
    AVVIST_STRENGT_FORTROLIG_ADRESSE,
    AVVIST_STRENGT_FORTROLIG_UTLAND,
    AVVIST_AVDØD,
    AVVIST_VERGEMÅL,
    AVVIST_PERSON_UTLAND,
    AVVIST_SKJERMING,
    AVVIST_FORTROLIG_ADRESSE,
    AVVIST_UKJENT_BOSTED,
    AVVIST_GEOGRAFISK,
    AVVIST_HABILITET,
    UKJENT,
    ;

    companion object {
        private val logger = LoggerFactory.getLogger(Avvisningskode::class.java)

        fun fraTitle(title: String?): Avvisningskode {
            val avvisningskode = entries.firstOrNull { it.name == title }
            if (avvisningskode == null) {
                logger.warn("Ukjent avvisningskode fra Tilgangsmaskinen: \"$title\"")
            }
            return avvisningskode ?: UKJENT
        }
    }
}

data class TilgangsmaskinResultat(
    val personIdent: String,
    val harTilgang: Boolean,
    val httpStatus: Int,
    val avvisningskode: Avvisningskode? = null,
    val begrunnelse: String? = null,
    val traceId: String? = null,
    val kanOverstyres: Boolean = false,
) {
    override fun toString(): String =
        "TilgangsmaskinResultat(personIdent=${personIdent.maskerIdent()}, harTilgang=$harTilgang, httpStatus=$httpStatus, " +
            "avvisningskode=$avvisningskode, begrunnelse=$begrunnelse, traceId=$traceId, kanOverstyres=$kanOverstyres)"
}

private fun String.maskerIdent(): String = "*".repeat(length)

class TilgangsmaskinException(
    message: String,
    cause: Throwable? = null,
    // Satt når feilen kom fra et HTTP-svar, slik at konsumenten kan skille klientfeil (4xx) fra serverfeil (5xx).
    val httpStatus: Int? = null,
) : RuntimeException(message, cause)
