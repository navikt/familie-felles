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
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.web.client.RestClient
import java.net.URI

internal class RetryOAuth2HttpClientTest {
    val requestFactory =
        ClientHttpRequestFactoryBuilder
            .detect()
            .build()

    val restClient =
        RestClient
            .builder()
            .requestFactory(requestFactory)
            .build()
    val client = RetryOAuth2HttpClient(restClient)

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
        stub(WireMock.notFound())
        post()
        wireMockServer.verify(1, RequestPatternBuilder.allRequests())
    }

    @Test
    internal fun `503 - skal prøve på nytt`() {
        stub(WireMock.serviceUnavailable())
        post()
        wireMockServer.verify(2, RequestPatternBuilder.allRequests())
    }

    @Test
    internal fun `502 - skal prøve på nytt`() {
        stub(WireMock.serverError().withStatus(502).withFault(Fault.CONNECTION_RESET_BY_PEER))
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
            WireMock
                .post(WireMock.anyUrl())
                .willReturn(responseDefinitionBuilder),
        )
    }

    private fun post(): Exception? =
        try {
            client.post(
                OAuth2HttpRequest
                    .builder(URI.create(wireMockServer.baseUrl()))
                    .oAuth2HttpHeaders(OAuth2HttpHeaders.builder().build())
                    .build(),
            )
            null
        } catch (e: Exception) {
            e
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
