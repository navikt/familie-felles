package no.nav.familie.tilgangsmaskin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

enum class Regeltype {
    KJERNE_REGELTYPE,
    KOMPLETT_REGELTYPE,
}

data class BrukerIdOgRegelsettDto(
    val brukerId: String,
    val type: Regeltype,
)

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
)

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
        fun fraTitle(title: String?): Avvisningskode = entries.firstOrNull { it.name == title } ?: UKJENT
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
)

class TilgangsmaskinException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
