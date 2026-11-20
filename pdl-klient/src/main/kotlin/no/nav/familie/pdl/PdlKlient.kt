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
) {
    @PublishedApi
    internal val graphqlUrl = URI.create(url.toString().trimEnd('/') + "/graphql")

    inline fun <reified T> hent(
        query: String,
        variabler: Map<String, Any?>,
    ): T {
        val respons =
            try {
                restClient
                    .post()
                    .uri(graphqlUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
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
) : PdlKlient(url, restClient)

class PdlHybridKlient(
    url: URI,
    restClient: RestClient,
) : PdlKlient(url, restClient)

class PdlMaskinKlient(
    url: URI,
    restClient: RestClient,
) : PdlKlient(url, restClient)
