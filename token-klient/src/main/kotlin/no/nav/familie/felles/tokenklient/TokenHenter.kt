package no.nav.familie.felles.tokenklient

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import kotlin.random.Random

internal object TokenHenter {
    fun hentToken(
        restClient: RestClient,
        tokenEndpoint: String,
        body: Map<String, String>,
    ): String =
        try {
            hentTokenFraEndpoint(restClient, tokenEndpoint, body)
        } catch (e: RuntimeException) {
            if (!kanPrøvePåNytt(e)) throw e

            log.warn("Tokenveksling feilet. Prøver igjen: status=${status(e)}")
            Thread.sleep(BASE_VENTETID_MS + Random.nextLong(-JITTER_MS, JITTER_MS + 1)) // tilfeldig ventetid 50-150ms
            hentTokenFraEndpoint(restClient, tokenEndpoint, body)
        }

    private fun hentTokenFraEndpoint(
        restClient: RestClient,
        tokenEndpoint: String,
        body: Map<String, String>,
    ): String {
        val response =
            restClient
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TokenResponse::class.java)
                ?: throw IllegalStateException("Fikk ikke svar fra token-endpoint")
        return response.accessToken
    }

    private fun kanPrøvePåNytt(exception: RuntimeException): Boolean =
        exception is ResourceAccessException ||
            exception is HttpServerErrorException

    private fun status(exception: RuntimeException): Int? = (exception as? HttpServerErrorException)?.statusCode?.value()

    private const val BASE_VENTETID_MS = 100L
    private const val JITTER_MS = 50L
    private val log = LoggerFactory.getLogger(TokenHenter::class.java)
}
