package no.nav.familie.webflux.sts

import no.nav.familie.webflux.filter.MdcValuesPropagatingFilterFunction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64

@Component
class StsTokenClient(
    @Value("\${STS_URL}") private val stsUrl: String,
    @Value("\${CREDENTIAL_USERNAME}") private val stsUsername: String,
    @Value("\${CREDENTIAL_PASSWORD}") private val stsPassword: String,
    @Value("\${STS_APIKEY:#{null}}") private val stsApiKey: String? = null
) {

    private val client = WebClient.builder()
        .baseUrl(stsUrl)
        .defaultHeader("Authorization", basicAuth(stsUsername, stsPassword)).apply {
            if (!stsApiKey.isNullOrEmpty()) {
                it.defaultHeader("x-nav-apiKey", stsApiKey)
            }
        }.filter(MdcValuesPropagatingFilterFunction())
        .build()

    private var cachedToken: AccessTokenResponse? = null

    private val isTokenValid: Boolean
        get() {
            if (cachedToken == null) {
                return false
            }
            log.debug(
                "Tokenet løper ut: {}. Tiden nå er: {}",
                Instant.ofEpochMilli(cachedToken!!.expires_in).atZone(ZoneId.systemDefault()).toLocalTime(),
                LocalTime.now(ZoneId.systemDefault())
            )

            return cachedToken!!.expires_in - MILLISEKUNDER_I_KVARTER > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        }

    val systemOIDCToken: String
        get() {
            if (isTokenValid) {
                log.debug("Henter token fra cache")
                return cachedToken!!.access_token
            }
            log.debug("Henter token fra STS")

            val accessTokenResponse = try {
                client.get().retrieve().bodyToMono(AccessTokenResponse::class.java).block(Duration.ofSeconds(30))
            } catch (e: RuntimeException) {
                throw StsAccessTokenFeilException("Feil i tilkobling", e)
            }

            if (accessTokenResponse != null) {
                cachedToken = accessTokenResponse
                return accessTokenResponse.access_token
            }
            throw StsAccessTokenFeilException("Manglende token")
        }

    companion object {

        private const val MILLISEKUNDER_I_KVARTER = 15 * 60 * 1000
        private val log = LoggerFactory.getLogger(StsTokenClient::class.java)
        private fun basicAuth(username: String, password: String): String {
            return "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        }
    }
}
