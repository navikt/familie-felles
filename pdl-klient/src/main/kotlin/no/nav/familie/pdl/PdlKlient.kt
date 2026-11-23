package no.nav.familie.pdl

import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.UnknownContentTypeException
import org.springframework.web.client.body
import tools.jackson.core.JacksonException
import java.net.URI

sealed class PdlKlient(
    url: URI,
    @PublishedApi internal val restClient: RestClient,
    @PublishedApi internal val behandlingsnummer: String?,
) {
    @PublishedApi
    internal val graphqlUrl = URI.create(url.toString().trimEnd('/') + "/graphql")

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
    ): T {
        require(behandlingsnummer.isNotBlank()) { "Behandlingsnummer kan ikke være blankt" }
        val respons =
            try {
                restClient
                    .post()
                    .uri(graphqlUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Behandlingsnummer", behandlingsnummer)
                    .body(PdlRequest(query = query, variables = variabler))
                    .retrieve()
                    .body<PdlRespons<T>>()
            } catch (feil: RestClientResponseException) {
                throw PdlException(
                    message = "Kall mot PDL feilet med HTTP-status ${feil.statusCode.value()}",
                    httpStatus = feil.statusCode.value(),
                    cause = feil,
                )
            } catch (feil: RestClientException) {
                if (feil is UnknownContentTypeException || feil.cause is HttpMessageConversionException) {
                    throw lesefeil(feil)
                }
                throw PdlException(cause = feil)
            } catch (feil: Exception) {
                throw PdlException(cause = feil)
            }

        val pdlRespons = respons ?: throw PdlException("Svar fra PDL mangler body")
        if (pdlRespons.errors.isNotEmpty()) {
            throw PdlException("Svar fra PDL inneholder feil", pdlRespons.errors)
        }
        return pdlRespons.data ?: throw PdlException("Svar fra PDL mangler data")
    }

    @PublishedApi
    internal fun konfigurertBehandlingsnummer(): String =
        behandlingsnummer
            ?: throw IllegalStateException(
                "Behandlingsnummer mangler. Send det med i kallet eller sett " +
                    "familie.felles.klient.pdl.behandlingsnummer",
            )

    @PublishedApi
    internal fun lesefeil(feil: Throwable): PdlException {
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
) : PdlKlient(url, restClient, behandlingsnummer)

class PdlHybridKlient(
    url: URI,
    restClient: RestClient,
    behandlingsnummer: String? = null,
) : PdlKlient(url, restClient, behandlingsnummer)

class PdlMaskinKlient(
    url: URI,
    restClient: RestClient,
    behandlingsnummer: String? = null,
) : PdlKlient(url, restClient, behandlingsnummer)
