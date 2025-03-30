package no.nav.familie.http.client

import no.nav.security.token.support.client.core.OAuth2ClientException
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.oauth2.DefaultOAuth2HttpClient
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpHeaders
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.SocketException
import java.net.SocketTimeoutException

class RetryOAuth2HttpClient(
    private val restClient: RestClient,
    private val maxRetries: Int = 2,
) : OAuth2HttpClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private val retryExceptions =
        setOf(
            SocketException::class,
            SocketTimeoutException::class,
            HttpServerErrorException.GatewayTimeout::class,
            HttpServerErrorException.BadGateway::class,
        )

    override fun post(oAuth2HttpRequest: OAuth2HttpRequest): OAuth2AccessTokenResponse {
        var retries = 0

        while (true) {
            try {
                return postRequest(oAuth2HttpRequest)
            } catch (e: Exception) {
                handleException(e, retries++, oAuth2HttpRequest)
            }
        }
    }

    /**
     * Kopi fra [DefaultOAuth2HttpClient]
     */
    private fun postRequest(req: OAuth2HttpRequest): OAuth2AccessTokenResponse =
        restClient
            .post()
            .uri(req.tokenEndpointUrl)
            .headers { it.addAll(headers(req)) }
            .body(
                LinkedMultiValueMap<String, String>().apply {
                    setAll(req.formParameters)
                },
            ).retrieve()
            .onStatus({ it.isError }) { _, response ->
                throw OAuth2ClientException("Received ${response.statusCode} from ${req.tokenEndpointUrl}")
            }.body<OAuth2AccessTokenResponse>() ?: throw OAuth2ClientException("No body in response from ${req.tokenEndpointUrl}")

    private fun headers(req: OAuth2HttpRequest): HttpHeaders = HttpHeaders().apply { putAll(req.oAuth2HttpHeaders.headers) }

    private fun handleException(
        e: Exception,
        retries: Int,
        oAuth2HttpRequest: OAuth2HttpRequest,
    ) {
        if (shouldRetry(e) && retries < maxRetries) {
            logger.warn(
                "Kall mot url=${oAuth2HttpRequest.tokenEndpointUrl} feilet, cause=${
                    NestedExceptionUtils.getMostSpecificCause(e)::class
                }",
            )
            secureLogger.warn("Kall mot url=${oAuth2HttpRequest.tokenEndpointUrl} feilet med feil=${e.message}")
        } else {
            throw e
        }
    }

    private fun shouldRetry(e: Exception): Boolean = retryExceptions.contains(e.cause?.let { it::class })
}
