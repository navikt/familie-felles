package no.nav.familie.http.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import no.nav.security.token.support.client.core.http.OAuth2HttpHeaders
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit

internal class RetryOAuth2HttpClientTest {
    private val restTemplateBuilder =
        RestTemplateBuilder()
            .setConnectTimeout(Duration.of(1, ChronoUnit.SECONDS))
            .setReadTimeout(Duration.of(1, ChronoUnit.SECONDS))

    private val client = RetryOAuth2HttpClient(restTemplateBuilder)

    @BeforeEach
    internal fun setUp() {
        wireMockServer.resetAll()
    }

    @Test
    internal fun `200 - skal kun kalle en gang`() {
        stub(WireMock.aResponse().withBody("{}"))
        post()
        wireMockServer.verify(1, RequestPatternBuilder.allRequests())
    }

    @Test
    internal fun `404 - skal kun kalle en gang`() {
        stub(WireMock.serverError().withStatus(404))
        post()
        wireMockServer.verify(1, RequestPatternBuilder.allRequests())
    }

    @Test
    internal fun `503 - skal prøve på nytt`() {
        stub(WireMock.aResponse().withStatus(503))
        post()
        wireMockServer.verify(2, RequestPatternBuilder.allRequests())
    }

    @Test
    internal fun `502 - skal prøve på nytt`() {
        stub(WireMock.serverError().withStatus(502))
        post()
        wireMockServer.verify(3, RequestPatternBuilder.allRequests())
    }

    @Test
    internal fun `socketException - skal prøve på nytt`() {
        stub(WireMock.serverError().withFault(Fault.CONNECTION_RESET_BY_PEER))
        post()
        wireMockServer.verify(3, RequestPatternBuilder.allRequests())
    }

    @Test
    internal fun `timeout - skal prøve på nytt ved timeout`() {
        stub(WireMock.serverError().withFixedDelay(2000))
        post()
        wireMockServer.verify(3, RequestPatternBuilder.allRequests())
    }

    private fun stub(responseDefinitionBuilder: ResponseDefinitionBuilder?) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.anyUrl())
                .willReturn(responseDefinitionBuilder),
        )
    }

    private fun post(): Exception? {
        return try {
            client.post(
                OAuth2HttpRequest.builder()
                    .tokenEndpointUrl(URI.create(wireMockServer.baseUrl()))
                    .oAuth2HttpHeaders(OAuth2HttpHeaders.builder().build())
                    .build(),
            )
            null
        } catch (e: Exception) {
            e
        }
    }

    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }
}
