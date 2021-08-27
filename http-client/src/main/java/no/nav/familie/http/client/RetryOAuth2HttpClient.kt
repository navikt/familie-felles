package no.nav.familie.http.client

import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.oauth2.DefaultOAuth2HttpClient
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.NestedExceptionUtils
import org.springframework.web.client.ResourceAccessException
import java.net.SocketException
import java.net.SocketTimeoutException

class RetryOAuth2HttpClient(restTemplateBuilder: RestTemplateBuilder,
                            private val maxRetries: Int = 2) : DefaultOAuth2HttpClient(restTemplateBuilder) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private val retryExceptions = setOf(SocketException::class,
                                        SocketTimeoutException::class)

    override fun post(oAuth2HttpRequest: OAuth2HttpRequest): OAuth2AccessTokenResponse {
        var retries = 0

        while (true) {
            try {
                return super.post(oAuth2HttpRequest)
            } catch (e: ResourceAccessException) {
                if (shouldRetry(e) && retries++ < maxRetries) {
                    logger.warn("Kall mot url=${oAuth2HttpRequest.tokenEndpointUrl} feilet, cause=${
                        NestedExceptionUtils.getMostSpecificCause(e)::class
                    }")
                    secureLogger.warn("Kall mot url=${oAuth2HttpRequest.tokenEndpointUrl} feilet med feil=${e.message}")
                } else {
                    throw e
                }
            }
        }
    }

    private fun shouldRetry(e: ResourceAccessException): Boolean {
        return retryExceptions.contains(e.cause?.let { it::class })
    }
}
