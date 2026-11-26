package no.nav.familie.saf

import no.nav.familie.felles.klient.retry.KlientRetryProperties
import no.nav.familie.felles.klient.retry.RetryKlient
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.ALL
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.UnknownContentTypeException
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.core.JacksonException
import java.net.URI

/**
 * Leser metadata og dokumenter fra Saf. Applikasjonen har ansvar for bruker- og temakontroll.
 *
 * Applikasjonen eier GraphQL-spørringer, paginering og DTO-er med riktig nullbarhet.
 * [SafException.httpStatus] er observert HTTP-status fra Saf eller tokenhenting. Ved GraphQL-feil er den utledet fra
 * `extensions.code`.
 */
sealed class SafKlient(
    url: URI,
    internal val restClient: RestClient,
    retryProperties: KlientRetryProperties,
) : RetryKlient(retryProperties) {
    val graphqlUrl =
        UriComponentsBuilder
            .fromUri(url)
            .pathSegment("graphql")
            .build()
            .toUri()

    val dokumentUrl =
        UriComponentsBuilder
            .fromUri(url)
            .pathSegment("rest", "hentdokument", "{journalpostId}", "{dokumentInfoId}", "{variantFormat}")
            .encode()
            .build()

    /** Sender [query] med [variabler] og returnerer `data` som [T]. */
    inline fun <reified T> hent(
        query: String,
        variabler: Map<String, Any?>,
    ): T = hent(query, variabler, object : ParameterizedTypeReference<SafRespons<T>>() {})

    @PublishedApi
    internal fun <T> hent(
        query: String,
        variabler: Map<String, Any?>,
        responstype: ParameterizedTypeReference<SafRespons<T>>,
    ): T =
        runCatching { hentRespons(query, variabler, responstype) }
            .fold(
                onSuccess = ::pakkUtData,
                onFailure = ::håndterThrowable,
            )

    private fun <T> hentRespons(
        query: String,
        variabler: Map<String, Any?>,
        responstype: ParameterizedTypeReference<SafRespons<T>>,
    ): SafRespons<T>? =
        retryVedMidlertidigFeil {
            restClient
                .post()
                .uri(graphqlUrl)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(SafRequest(query, variabler))
                .retrieve()
                .body(responstype)
        }

    private fun <T> pakkUtData(respons: SafRespons<T>?): T =
        when {
            respons == null -> throw SafException("Svar fra Saf mangler body")

            respons.errors.isNotEmpty() -> throw SafException(
                message = "Svar fra Saf inneholder feil",
                errors = respons.errors,
                httpStatus = utledHttpStatus(respons.errors),
            )

            respons.data == null -> throw SafException("Svar fra Saf mangler data")

            else -> respons.data
        }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        variantFormat: SafVariantFormat = SafVariantFormat.ARKIV,
    ): ByteArray =
        runCatching {
            val dokumentUrl =
                dokumentUrl
                    .expand(journalpostId, dokumentInfoId, variantFormat.name)
                    .toUri()

            retryVedMidlertidigFeil {
                restClient
                    .get()
                    .uri(dokumentUrl)
                    .accept(ALL)
                    .retrieve()
                    .body<ByteArray>()
            } ?: throw SafException("Svar fra Saf mangler body")
        }.getOrElse(::håndterThrowable)

    private fun håndterThrowable(feil: Throwable): Nothing =
        throw when (feil) {
            is SafException -> {
                feil
            }

            is RestClientResponseException -> {
                SafException(
                    message = "Kall mot Saf feilet med HTTP-status ${feil.statusCode.value()}",
                    httpStatus = feil.statusCode.value(),
                    cause = feil,
                )
            }

            is Exception if inneholderLesefeil(feil) -> {
                lesefeil(feil)
            }

            is Exception -> {
                SafException(cause = feil)
            }

            // Error kastes videre uendret.
            else -> {
                feil
            }
        }

    private fun inneholderLesefeil(feil: Throwable): Boolean =
        generateSequence(feil) { it.cause }.any {
            it is UnknownContentTypeException || it is HttpMessageConversionException || it is JacksonException
        }

    private fun lesefeil(feil: Throwable): SafException {
        val stier =
            generateSequence(feil) { it.cause }
                .filterIsInstance<JacksonException>()
                .mapNotNull {
                    it.path
                        .joinToString(".") { it.propertyName ?: "[${it.index}]" }
                        .takeIf(String::isNotEmpty)
                }.toList()
        return SafException("Svar fra Saf kunne ikke leses${if (stier.isNotEmpty()) ": $stier" else ""}")
    }

    private fun utledHttpStatus(errors: List<SafFeil>): Int? {
        val koder = errors.mapNotNull { it.extensions?.code }.toSet()
        return HTTP_STATUS_FOR_GRAPHQL_KODE.entries.firstOrNull { it.key in koder }?.value
    }

    private companion object {
        // Rekkefølgen er prioriteten når svaret har flere feil
        val HTTP_STATUS_FOR_GRAPHQL_KODE =
            linkedMapOf(
                "forbidden" to 403,
                "not_found" to 404,
                "bad_request" to 400,
                "server_error" to 500,
            )
    }
}

class SafOboKlient(
    url: URI,
    restClient: RestClient,
    retry: KlientRetryProperties = KlientRetryProperties(),
) : SafKlient(url, restClient, retry)
