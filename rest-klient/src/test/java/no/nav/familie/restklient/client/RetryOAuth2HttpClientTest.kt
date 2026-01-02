package no.nav.familie.restklient.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import no.nav.familie.restklient.client.RetryOAuth2HttpClient
import no.nav.security.token.support.client.core.http.OAuth2HttpHeaders
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RetryOAuth2HttpClientTest {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var client: RetryOAuth2HttpClient

    @BeforeAll
    fun setupServer() {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer.start()

        val requestFactory =
            HttpComponentsClientHttpRequestFactory().apply {
                setConnectionRequestTimeout(1000)
                setReadTimeout(1000)
            }

        val restClient =
            RestClient
                .builder()
                .requestFactory(requestFactory)
                .build()

        client = RetryOAuth2HttpClient(restClient)
    }

    @AfterAll
    fun tearDown() {
        wireMockServer.stop()
    }

    @BeforeEach
    fun resetMocks() {
        wireMockServer.resetAll()
    }

    @Test
    fun `200 - skal kun kalle en gang`() = stubAndPost(WireMock.aResponse().withBody("{}"), expectedCalls = 1)

    @Test
    fun `404 - skal kun kalle en gang`() = stubAndPost(WireMock.notFound(), expectedCalls = 1)

    @Test
    fun `503 - skal prøve på nytt`() = stubAndPost(WireMock.serviceUnavailable(), expectedCalls = 2)

    @Test
    fun `502 - skal prøve på nytt`() =
        stubAndPost(
            WireMock.serverError().withStatus(502).withFault(Fault.CONNECTION_RESET_BY_PEER),
            expectedCalls = 3,
        )

    @Test
    fun `socketException - skal prøve på nytt`() =
        stubAndPost(
            WireMock.serverError().withFault(Fault.CONNECTION_RESET_BY_PEER),
            expectedCalls = 3,
        )

    @Test
    fun `timeout - skal prøve på nytt ved timeout`() =
        stubAndPost(
            WireMock.serverError().withFixedDelay(2000),
            expectedCalls = 3,
        )

    private fun stubAndPost(
        response: ResponseDefinitionBuilder,
        expectedCalls: Int,
    ) {
        wireMockServer.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(response))
        try {
            client.post(
                OAuth2HttpRequest
                    .builder(URI.create(wireMockServer.baseUrl()))
                    .oAuth2HttpHeaders(
                        OAuth2HttpHeaders
                            .builder()
                            .build(),
                    ).build(),
            )
        } catch (_: Exception) {
        }
        wireMockServer.verify(expectedCalls, WireMock.postRequestedFor(WireMock.anyUrl()))
    }
}
