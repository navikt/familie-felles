package no.nav.familie.tilgangsmaskin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Regelsett i Tilgangsmaskinen.
 *
 * [KJERNE_REGELTYPE] dekker kode 6, kode 7, §19, skjerming (egen ansatt), egne data og egen familie.
 * [KOMPLETT_REGELTYPE] legger i tillegg på de overstyrbare reglene for geografi/enhet, ukjent bosted,
 * utland, avdød og vergemål.
 */
enum class Regeltype {
    KJERNE_REGELTYPE,
    KOMPLETT_REGELTYPE,
}

data class BrukerIdOgRegelsettDto(
    val brukerId: String,
    val type: Regeltype,
)

/**
 * NB: [resultater] har bevisst ingen defaultverdi.
 *
 * Får alle konstruktørparametrene i en Kotlin data class en defaultverdi, genererer Kotlin en syntetisk
 * no-arg-konstruktør. Jackson foretrekker den dersom Kotlin-modulen ikke er registrert på ObjectMapperen,
 * og fyller da aldri feltene — vi ville fått en tom resultatliste uten feilmelding, og dermed nektet
 * tilgang for alle. Uten defaultverdi feiler deserialiseringen høylytt i stedet for stille.
 */
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

// title har bevisst ingen defaultverdi — se forklaringen på TilgangsmaskinBulkResponsDto.
@JsonIgnoreProperties(ignoreUnknown = true)
data class TilgangsmaskinAvvisningDto(
    // title leses som String, ikke enum. Tilgangsmaskinen kan innføre nye avvisningskoder, og en ukjent
    // verdi skal ikke føre til at hele bulk-responsen feiler å deserialisere.
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

/**
 * Resultat av tilgangssjekk for én person.
 *
 * [avvisningskode] er null når tilgang er innvilget, slik at «innvilget» og «avvist av ukjent grunn» ikke
 * kan forveksles. [kanOverstyres] er bevisst tatt vare på: uten den kan ikke konsumenten skille avvisninger
 * som kan overstyres (geografi/enhet) fra de som ikke kan det (kode 6/7, skjerming).
 */
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
