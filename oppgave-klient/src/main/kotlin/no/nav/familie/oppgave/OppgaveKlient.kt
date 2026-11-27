package no.nav.familie.oppgave

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.oppgave.modell.FinnOppgaverRequest
import no.nav.familie.oppgave.modell.Oppgave
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import no.nav.familie.oppgave.modell.PatchOppgaveRequest
import no.nav.familie.oppgave.modell.SokOppgaverResponse
import org.slf4j.MDC
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.UnknownContentTypeException
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed class OppgaveKlient(
    private val url: URI,
    private val restClient: RestClient,
) {
    private val feilMapper = jacksonObjectMapper()

    fun finnOppgaver(
        request: FinnOppgaverRequest,
        korrelasjonsId: String? = null,
    ): SokOppgaverResponse =
        utfør(OppgaveOperasjon.FINN_OPPGAVER, korrelasjonsId) { valgtKorrelasjonsId ->
            val uri = byggSokeUri(request)
            restClient
                .get()
                .uri(uri.mal) { _ -> uri.verdi }
                .accept(APPLICATION_JSON)
                .header(KORRELASJONS_ID_HEADER, valgtKorrelasjonsId)
                .retrieve()
                .body<SokOppgaverResponse>()
        }

    /**
     * Oppretter en oppgave, eller henter den eksisterende oppgaven ved UUID-duplikat (HTTP 409 med oppgaveId).
     * Konsumenten må bruke samme request-UUID for samme logiske oppretting ved retry og etter omstart.
     * Serveren sammenligner bare UUID, ikke requestinnhold. En gjenfunnet oppgave returneres i sin nåværende tilstand.
     * Klienten gjentar ikke POST automatisk. Feil ved gjenfinning sendes videre som feil fra GET-kallet.
     * Ukjente enumverdier i POST-responsen eller GET etter 409 gir UGYLDIG_RESPONS selv om oppgaven er lagret.
     * Et nytt opprettingsforsøk må fortsatt bruke samme UUID.
     */
    fun opprettOppgave(
        request: OpprettOppgaveRequest,
        korrelasjonsId: String? = null,
    ): Oppgave =
        try {
            utfør(OppgaveOperasjon.OPPRETT_OPPGAVE, korrelasjonsId) { valgtKorrelasjonsId ->
                restClient
                    .post()
                    .uri(byggUri())
                    .accept(APPLICATION_JSON)
                    .contentType(APPLICATION_JSON)
                    .header(KORRELASJONS_ID_HEADER, valgtKorrelasjonsId)
                    .body(request)
                    .retrieve()
                    .body<Oppgave>()
            }
        } catch (feil: OppgaveException) {
            if (feil.httpStatus != 409) throw feil
            val httpFeil = feil.cause as? RestClientResponseException ?: throw feil
            val oppgaveId = lesEksisterendeOppgaveId(httpFeil) ?: throw feil

            hentOppgave(oppgaveId, feil.korrelasjonsId)
        }

    fun hentOppgave(
        id: Long,
        korrelasjonsId: String? = null,
    ): Oppgave =
        utfør(OppgaveOperasjon.HENT_OPPGAVE, korrelasjonsId) { valgtKorrelasjonsId ->
            restClient
                .get()
                .uri(byggUri("/$id"))
                .accept(APPLICATION_JSON)
                .header(KORRELASJONS_ID_HEADER, valgtKorrelasjonsId)
                .retrieve()
                .body<Oppgave>()
        }

    fun patchOppgave(
        id: Long,
        request: PatchOppgaveRequest,
        korrelasjonsId: String? = null,
    ): Oppgave =
        utfør(OppgaveOperasjon.PATCH_OPPGAVE, korrelasjonsId) { valgtKorrelasjonsId ->
            restClient
                .patch()
                .uri(byggUri("/$id"))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header(KORRELASJONS_ID_HEADER, valgtKorrelasjonsId)
                .body(request)
                .retrieve()
                .body<Oppgave>()
        }

    private fun <T> utfør(
        operasjon: OppgaveOperasjon,
        korrelasjonsId: String?,
        kall: (String) -> T?,
    ): T {
        val valgtKorrelasjonsId =
            korrelasjonsId ?: MDC.get(MDCConstants.MDC_CALL_ID) ?: UUID.randomUUID().toString()
        return runCatching { kall(valgtKorrelasjonsId) }.fold(
            onSuccess = { respons ->
                respons ?: throw OppgaveException(operasjon, OppgaveFeiltype.UGYLDIG_RESPONS, null, valgtKorrelasjonsId)
            },
            onFailure = { feil -> håndterThrowable(feil, operasjon, valgtKorrelasjonsId) },
        )
    }

    private fun håndterThrowable(
        feil: Throwable,
        operasjon: OppgaveOperasjon,
        korrelasjonsId: String,
    ): Nothing =
        throw when (feil) {
            is RestClientResponseException -> {
                OppgaveException(
                    operasjon,
                    OppgaveFeiltype.HTTP,
                    feil.statusCode.value(),
                    korrelasjonsId,
                    message = lesFeilmelding(feil),
                    cause = feil,
                )
            }

            is UnknownContentTypeException -> {
                OppgaveException(operasjon, OppgaveFeiltype.UGYLDIG_RESPONS, null, korrelasjonsId, cause = feil)
            }

            is RestClientException if feil.cause is HttpMessageConversionException -> {
                OppgaveException(operasjon, OppgaveFeiltype.UGYLDIG_RESPONS, null, korrelasjonsId, cause = feil)
            }

            is Exception -> {
                if (feil is InterruptedException) Thread.currentThread().interrupt()
                OppgaveException(operasjon, OppgaveFeiltype.TRANSPORT, null, korrelasjonsId, cause = feil)
            }

            else -> {
                feil
            }
        }

    private fun lesFeilmelding(feil: RestClientResponseException): String? =
        runCatching {
            feilMapper
                .readTree(feil.responseBodyAsString)
                ?.get("feilmelding")
                ?.takeIf { it.isString }
                ?.asString()
                ?.takeIf(String::isNotBlank)
        }.getOrNull()

    private fun lesEksisterendeOppgaveId(feil: RestClientResponseException): Long? =
        runCatching {
            feilMapper
                .readTree(feil.responseBodyAsString)
                ?.get("oppgaveId")
                ?.takeIf { it.isIntegralNumber && it.canConvertToLong() }
                ?.asLong()
                ?.takeIf { it > 0 }
        }.getOrNull()

    /**
     * Returnerer en URI til logging hos konsumenten, uten query, fragment og brukerinformasjon.
     */
    fun uriForLogging(oppgaveId: Long? = null): URI =
        UriComponentsBuilder
            .fromUri(byggUri(oppgaveId?.let { "/$it" } ?: ""))
            .replaceQuery(null)
            .fragment(null)
            .userInfo(null)
            .build(true)
            .toUri()

    private fun byggUri(endepunkt: String = ""): URI =
        UriComponentsBuilder
            .fromUri(url)
            .path("api/v1/oppgaver$endepunkt")
            .build()
            .encode()
            .toUri()

    private class SokeUri(
        val mal: String,
        val verdi: URI,
    )

    private fun byggSokeUri(request: FinnOppgaverRequest): SokeUri {
        val builder =
            UriComponentsBuilder
                .fromUri(url)
                .path("api/v1/oppgaver")
        val queryValues = mutableMapOf<String, Any>()
        var placeholderIndex = 0

        fun leggTil(
            nokkel: String,
            verdi: String,
        ) {
            val placeholder = "query${placeholderIndex++}"
            builder.queryParam(nokkel, "{$placeholder}")
            queryValues[placeholder] = verdi
        }

        request.statuskategori?.let { leggTil("statuskategori", it.name) }
        request.tema?.forEach { leggTil("tema", it) }
        request.oppgavetype?.forEach { leggTil("oppgavetype", it) }
        request.aktoerId?.forEach { leggTil("aktoerId", it) }
        request.journalpostId?.forEach { leggTil("journalpostId", it) }
        request.saksreferanse?.forEach { leggTil("saksreferanse", it) }
        request.orgnr?.forEach { leggTil("orgnr", it) }
        request.tildeltEnhetsnr?.let { leggTil("tildeltEnhetsnr", it) }
        request.tilordnetRessurs?.let { leggTil("tilordnetRessurs", it) }
        request.behandlingstema?.let { leggTil("behandlingstema", it) }
        request.behandlingstype?.let { leggTil("behandlingstype", it) }
        request.ferdigstiltFom?.let { leggTil("ferdigstiltFom", it.format(LOKAL_DATO_TID_FORMAT)) }
        request.ferdigstiltTom?.let { leggTil("ferdigstiltTom", it.format(LOKAL_DATO_TID_FORMAT)) }
        request.limit?.let { leggTil("limit", it.toString()) }
        request.offset?.let { leggTil("offset", it.toString()) }
        request.status?.forEach { leggTil("status", it.name) }
        request.tildeltRessurs?.let { leggTil("tildeltRessurs", it.toString()) }
        request.erUtenMappe?.let { leggTil("erUtenMappe", it.toString()) }
        request.mappeId?.forEach { leggTil("mappeId", it.toString()) }
        request.opprettetAv?.let { leggTil("opprettetAv", it) }
        request.opprettetAvEnhetsnr?.let { leggTil("opprettetAvEnhetsnr", it) }
        request.behandlesAvApplikasjon?.let { leggTil("behandlesAvApplikasjon", it) }
        request.aktivDatoFom?.let { leggTil("aktivDatoFom", it.toString()) }
        request.aktivDatoTom?.let { leggTil("aktivDatoTom", it.toString()) }
        request.opprettetFom?.let { leggTil("opprettetFom", it.format(LOKAL_DATO_TID_FORMAT)) }
        request.opprettetTom?.let { leggTil("opprettetTom", it.format(LOKAL_DATO_TID_FORMAT)) }
        request.fristFom?.let { leggTil("fristFom", it.toString()) }
        request.fristTom?.let { leggTil("fristTom", it.toString()) }
        request.bnr?.forEach { leggTil("bnr", it) }
        request.samhandlernr?.forEach { leggTil("samhandlernr", it) }
        request.metadatanokkel?.let { leggTil("metadatanokkel", it.name) }
        request.metadataverdi?.forEach { leggTil("metadataverdi", it) }
        request.sorteringsrekkefolge?.let { leggTil("sorteringsrekkefolge", it.name) }
        request.sorteringsfelt?.let { leggTil("sorteringsfelt", it.name) }
        request.presisMatchTemakategorisering?.let { leggTil("presisMatchTemakategorisering", it.toString()) }

        val mal = builder.build().toUriString()
        return SokeUri(mal, builder.encode().buildAndExpand(queryValues).toUri())
    }

    companion object {
        private const val KORRELASJONS_ID_HEADER = "X-Correlation-ID"
        private val LOKAL_DATO_TID_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }
}

class OppgaveOboKlient(
    url: URI,
    restClient: RestClient,
) : OppgaveKlient(url, restClient)

class OppgaveHybridKlient(
    url: URI,
    restClient: RestClient,
) : OppgaveKlient(url, restClient)

class OppgaveMaskinKlient(
    url: URI,
    restClient: RestClient,
) : OppgaveKlient(url, restClient)
