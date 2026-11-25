package no.nav.familie.pdl

import no.nav.familie.felles.klient.retry.KlientRetry
import no.nav.familie.felles.klient.retry.KlientRetryProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.UnknownContentTypeException
import tools.jackson.core.JacksonException
import java.net.URI

sealed class PdlKlient(
    url: URI,
    internal val restClient: RestClient,
    internal val behandlingsnummer: String?,
    retryKonfig: KlientRetryProperties,
) {
    private val retry = KlientRetry(retryKonfig)

    /** Adressen klienten sender GraphQL-spørringene til. */
    val graphqlUrl: URI = URI.create(url.toString().trimEnd('/') + "/graphql")

    /**
     * Sender [query] med [variabler] til PDL og returnerer `data` som [T].
     *
     * Uten [behandlingsnummer] brukes `familie.felles.klient.pdl.behandlingsnummer`. Er den ikke satt, feiler
     * kallet med [IllegalStateException] før noe sendes til PDL. Et blankt [behandlingsnummer] gir
     * [IllegalArgumentException].
     */
    inline fun <reified T> hent(
        query: String,
        variabler: Map<String, Any?>,
        behandlingsnummer: String = konfigurertBehandlingsnummer(),
    ): T = hent(query, variabler, behandlingsnummer, object : ParameterizedTypeReference<PdlRespons<T>>() {})

    /** Som `hent` med reified type, men tar inn [responstype]. */
    @PublishedApi
    internal fun <T> hent(
        query: String,
        variabler: Map<String, Any?>,
        behandlingsnummer: String,
        responstype: ParameterizedTypeReference<PdlRespons<T>>,
    ): T {
        require(behandlingsnummer.isNotBlank()) { "Behandlingsnummer kan ikke være blankt" }
        return runCatching { hentRespons(query, variabler, behandlingsnummer, responstype) }
            .fold(
                onSuccess = ::pakkUtData,
                onFailure = ::håndterThrowable,
            )
    }

    private fun <T> hentRespons(
        query: String,
        variabler: Map<String, Any?>,
        behandlingsnummer: String,
        responstype: ParameterizedTypeReference<PdlRespons<T>>,
    ): PdlRespons<T>? =
        retry.utfør {
            restClient
                .post()
                .uri(graphqlUrl)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .header("Behandlingsnummer", behandlingsnummer)
                .body(PdlRequest(query, variabler))
                .retrieve()
                .body(responstype)
        }

    @PublishedApi
    internal fun konfigurertBehandlingsnummer(): String =
        behandlingsnummer
            ?: throw IllegalStateException(
                "Behandlingsnummer mangler. Send det med i kallet eller sett " +
                    "familie.felles.klient.pdl.behandlingsnummer",
            )

    private fun <T> pakkUtData(respons: PdlRespons<T>?): T =
        when {
            respons == null -> throw PdlException("Svar fra PDL mangler body")
            respons.errors.isNotEmpty() -> throw PdlException("Svar fra PDL inneholder feil", respons.errors)
            respons.data == null -> throw PdlException("Svar fra PDL mangler data")
            else -> respons.data
        }

    private fun håndterThrowable(feil: Throwable): Nothing =
        throw when (feil) {
            is RestClientResponseException -> {
                PdlException(
                    message = "Kall mot PDL feilet med HTTP-status ${feil.statusCode.value()}",
                    httpStatus = feil.statusCode.value(),
                    cause = feil,
                )
            }

            is UnknownContentTypeException -> {
                lesefeil(feil)
            }

            is RestClientException if feil.cause is HttpMessageConversionException -> {
                lesefeil(feil)
            }

            is Exception -> {
                PdlException(cause = feil)
            }

            // Error pakkes bevisst ikke inn, men kastes videre uendret.
            else -> {
                feil
            }
        }

    private fun lesefeil(feil: Throwable): PdlException {
        val stier =
            generateSequence(feil) { it.cause }
                .filterIsInstance<JacksonException>()
                .mapNotNull {
                    it.path
                        .joinToString(".") { it.propertyName ?: "[${it.index}]" }
                        .takeIf(String::isNotEmpty)
                }.toList()
        return PdlException("Svar fra PDL kunne ikke leses${if (stier.isNotEmpty()) ": $stier" else ""}")
    }
}

class PdlOboKlient(
    url: URI,
    restClient: RestClient,
    behandlingsnummer: String? = null,
    retry: KlientRetryProperties = KlientRetryProperties(),
) : PdlKlient(url, restClient, behandlingsnummer, retry)

class PdlHybridKlient(
    url: URI,
    restClient: RestClient,
    behandlingsnummer: String? = null,
    retry: KlientRetryProperties = KlientRetryProperties(),
) : PdlKlient(url, restClient, behandlingsnummer, retry)

class PdlMaskinKlient(
    url: URI,
    restClient: RestClient,
    behandlingsnummer: String? = null,
    retry: KlientRetryProperties = KlientRetryProperties(),
) : PdlKlient(url, restClient, behandlingsnummer, retry)
