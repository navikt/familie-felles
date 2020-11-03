package no.nav.familie.http.sts


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class StsRestClientTest {

    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(4040))

    private lateinit var stsRestClient: StsRestClient

    @BeforeEach
    fun setUp() {
        val objectMapper = ObjectMapper().registerModule(KotlinModule())
        wireMockServer.start()
        stsRestClient = StsRestClient(objectMapper, URI.create("http://localhost:4040"), "username", "password")
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `skal først hente token fra sts, og så bruke cache på neste`() {
        wireMockServer.stubFor(
                get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token1\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\"}")))


        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
        wireMockServer.resetAll()
        wireMockServer.stubFor(
                get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token2\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\"}")))
        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
    }

    @Test
    fun `skal først hente token fra sts, og så hente ny fordi token er utløpt`() {
        wireMockServer.stubFor(
                get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token1\", \"token_type\": \"Bearer\", \"expires_in\": \"1\"}")))


        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
        Thread.sleep(1000)
        wireMockServer.resetAll()
        wireMockServer.stubFor(
                get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token2\", \"token_type\": \"Bearer\", \"expires_in\": \"1\"}")))
        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token2")
    }
}
