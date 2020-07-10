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
import java.net.http.HttpResponse
import java.time.*
import java.util.*
import java.util.concurrent.ExecutionException

@Component
class StsRestClient(private val mapper: ObjectMapper,
                    @Value("\${STS_URL}") private val stsUrl: URI,
                    @Value("\${CREDENTIAL_USERNAME}") private val stsUsername: String,
                    @Value("\${CREDENTIAL_PASSWORD}") private val stsPassword: String,
                    @Value("\${STS_APIKEY}") private val stsApiKey: String? = null) {

    private val client: HttpClient = HttpClientUtil.create()

    private var cachedToken: AccessTokenResponse? = null

    private val isTokenValid: Boolean
        get() {
            if (cachedToken == null) {
                return false
            }
            log.debug("Tokenet løper ut: {}. Tiden nå er: {}",
                      Instant.ofEpochMilli(cachedToken!!.expires_in).atZone(ZoneId.systemDefault()).toLocalTime(),
                      LocalTime.now(ZoneId.systemDefault()))

            return cachedToken!!.expires_in - MILLISEKUNDER_I_KVARTER > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        }

    val systemOIDCToken: String
        get() {
            if (isTokenValid) {
                log.debug("Henter token fra cache")
                return cachedToken!!.access_token
            }
            log.debug("Henter token fra STS")
            val builder =
                    HttpRequestUtil.createRequest(basicAuth(stsUsername, stsPassword))
                            .uri(stsUrl)
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(30))

            if (stsApiKey != null) {
                builder.header("x-nav-apiKey", stsApiKey)
            }
            val request = builder.build()

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
        private const val MILLISEKUNDER_I_KVARTER = 15 * 60 * 1000
        private val log = LoggerFactory.getLogger(StsRestClient::class.java)
        private fun basicAuth(username: String, password: String): String {
            return "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        }
    }

}
