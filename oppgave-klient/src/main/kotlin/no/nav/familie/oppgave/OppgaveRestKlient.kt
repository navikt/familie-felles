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

sealed class OppgaveRestKlient(
    private val url: URI,
    private val restClient: RestClient,
) : OppgaveKlient {
    private val feilMapper = jacksonObjectMapper()

    final override fun finnOppgaver(
        request: FinnOppgaverRequest,
        korrelasjonsId: String?,
    ): SokOppgaverResponse =
        utfør(OppgaveOperasjon.FINN_OPPGAVER, korrelasjonsId) { valgtKorrelasjonsId ->
            val uri = byggSokeUri(request)
            restClient
                .get()
                .uri(uri)
                .accept(APPLICATION_JSON)
                .header(KORRELASJONS_ID_HEADER, valgtKorrelasjonsId)
                .retrieve()
                .body<SokOppgaverResponse>()
        }

    final override fun opprettOppgave(
        request: OpprettOppgaveRequest,
        korrelasjonsId: String?,
    ): Oppgave =
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

    final override fun hentOppgave(
        id: Long,
        korrelasjonsId: String?,
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

    final override fun patchOppgave(
        id: Long,
        request: PatchOppgaveRequest,
        korrelasjonsId: String?,
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

    // Rød sone: feilrespons og original cause bevares, men skal ikke logges automatisk her.
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

    private fun byggUri(endepunkt: String = ""): URI =
        UriComponentsBuilder
            .fromUriString("${url.toString().trimEnd('/')}/api/v1/oppgaver$endepunkt")
            .build()
            .encode()
            .toUri()

    private fun byggSokeUri(request: FinnOppgaverRequest): URI {
        val builder =
            UriComponentsBuilder
                .fromUriString("${url.toString().trimEnd('/')}/api/v1/oppgaver")
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

        request.statuskategori?.let { leggTil("statuskategori", it) }
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

        return builder.encode().buildAndExpand(queryValues).toUri()
    }

    companion object {
        private const val KORRELASJONS_ID_HEADER = "X-Correlation-ID"
        private val LOKAL_DATO_TID_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }
}

class OppgaveOboKlient(
    url: URI,
    restClient: RestClient,
) : OppgaveRestKlient(url, restClient)

class OppgaveHybridKlient(
    url: URI,
    restClient: RestClient,
) : OppgaveRestKlient(url, restClient)

class OppgaveMaskinKlient(
    url: URI,
    restClient: RestClient,
) : OppgaveRestKlient(url, restClient)
