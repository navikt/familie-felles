package no.nav.familie.http.sts

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.http.client.HttpClientUtil
import no.nav.familie.http.client.HttpRequestUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.concurrent.ExecutionException

@Component
class StsRestClient(private val mapper: ObjectMapper,
                    @Value("\${STS_URL}") private val stsUrl: URI,
                    @Value("\${CREDENTIAL_USERNAME}") private val stsUsername: String,
                    @Value("\${CREDENTIAL_PASSWORD}") private val stsPassword: String,
                    @Value("\${STS_APIKEY:#{null}}") private val stsApiKey: String? = null) {

    private val client: HttpClient = HttpClientUtil.create()

    private var cachedToken: AccessTokenResponse? = null
    private var refreshCachedTokenTidspunkt = LocalDateTime.now()

    private val isTokenValid: Boolean
        get() {
            if (cachedToken == null) {
                return false
            }
            log.debug("Skal refreshe token: {}. Tiden nå er: {}",
                      refreshCachedTokenTidspunkt,
                      LocalTime.now())

            return refreshCachedTokenTidspunkt.isAfter(LocalDateTime.now())
        }

    val systemOIDCToken: String
        get() {
            if (isTokenValid) {
                log.debug("Henter token fra cache")
                return cachedToken!!.access_token
            }
            log.debug("Henter token fra STS")
            val request =
                    HttpRequestUtil.createRequest(basicAuth(stsUsername, stsPassword))
                            .uri(stsUrl)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .timeout(Duration.ofSeconds(30)).apply {
                                if (!stsApiKey.isNullOrEmpty()) {
                                    header("x-nav-apiKey", stsApiKey)
                                }
                            }.build()

            val accessTokenResponse = try {
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply { obj: HttpResponse<String?> -> obj.body() }
                        .thenApply { it: String? ->
                            håndterRespons(it)
                        }
                        .get()
            } catch (e: InterruptedException) {
                throw StsAccessTokenFeilException("Feil i tilkobling", e)
            } catch (e: ExecutionException) {
                throw StsAccessTokenFeilException("Feil i tilkobling", e)
            }
            if (accessTokenResponse != null) {
                cachedToken = accessTokenResponse
                refreshCachedTokenTidspunkt = LocalDateTime.now()
                        .plusSeconds(accessTokenResponse.expires_in)
                        .minusSeconds(accessTokenResponse.expires_in / 4) // Trekker av 1/4. Refresher etter 3/4 av levetiden
                return accessTokenResponse.access_token
            }
            throw StsAccessTokenFeilException("Manglende token")
        }

    private fun håndterRespons(it: String?): AccessTokenResponse {
        return try {
            mapper.readValue(it, AccessTokenResponse::class.java)
        } catch (e: IOException) {
            throw StsAccessTokenFeilException("Parsing av respons feilet", e)
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(StsRestClient::class.java)
        private fun basicAuth(username: String, password: String): String {
            return "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        }
    }

}
