package no.nav.familie.felles.tokenklient.sts

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDateTime
import java.util.Base64

@Component
class StsRestClient(
    @Value("\${STS_URL}") private val stsUrl: String,
    @Value("\${CREDENTIAL_USERNAME}") private val stsUsername: String,
    @Value("\${CREDENTIAL_PASSWORD}") private val stsPassword: String,
    @Value("\${STS_APIKEY:#{null}}") private val stsApiKey: String? = null,
    restClientBuilder: RestClient.Builder = RestClient.builder(),
) {
    private val restClient: RestClient =
        restClientBuilder
            .baseUrl(stsUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(stsUsername, stsPassword))
            .apply {
                if (!stsApiKey.isNullOrEmpty()) {
                    it.defaultHeader("x-nav-apiKey", stsApiKey)
                }
            }.build()

    private var cachedToken: StsTokenResponse? = null
    private var refreshCachedTokenTidspunkt = LocalDateTime.now()

    private val isTokenValid: Boolean
        get() {
            if (cachedToken == null) return false
            log.debug("Skal refreshe token: {}. Nå: {}", refreshCachedTokenTidspunkt, LocalDateTime.now())
            return refreshCachedTokenTidspunkt.isAfter(LocalDateTime.now())
        }

    val systemOIDCToken: String
        get() {
            if (isTokenValid) {
                log.debug("Henter token fra cache")
                return cachedToken!!.accessToken
            }

            log.debug("Henter token fra STS")
            val response =
                restClient
                    .post()
                    .retrieve()
                    .body(StsTokenResponse::class.java)
                    ?: throw StsAccessTokenFeilException("Fikk ikke svar fra STS")

            cachedToken = response
            refreshCachedTokenTidspunkt =
                LocalDateTime
                    .now()
                    .plusSeconds(response.expiresIn)
                    .minusSeconds(response.expiresIn / 4)

            return response.accessToken
        }

    companion object {
        private val log = LoggerFactory.getLogger(StsRestClient::class.java)

        private fun basicAuth(
            username: String,
            password: String,
        ): String = "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
    }
}
