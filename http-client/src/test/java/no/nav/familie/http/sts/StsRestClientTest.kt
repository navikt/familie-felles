package no.nav.familie.http.sts


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.net.URI

class StsRestClientTest {

    companion object {

        private lateinit var stsRestClient: StsRestClient
        private val objectMapper = ObjectMapper().registerModule(KotlinModule())
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

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
    }

    @BeforeEach
    fun setupEachTest() {
        stsRestClient =
                StsRestClient(objectMapper, URI.create("http://localhost:${wireMockServer.port()}"), "username", "password")
    }


    @Test
    fun `skal først hente token fra sts, og så bruke cache på neste`() {
        wireMockServer.stubFor(
                post(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token1\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\"}")))


        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
        wireMockServer.resetAll()
        wireMockServer.stubFor(
                post(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token2\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\"}")))
        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
    }

    @Test
    fun `skal først hente token fra sts, og så hente ny fordi token er utløpt`() {
        wireMockServer.stubFor(
                post(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token1\", \"token_type\": \"Bearer\", \"expires_in\": \"1\"}")))


        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
        Thread.sleep(1000)
        wireMockServer.resetAll()
        wireMockServer.stubFor(
                post(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"access_token\": \"token2\", \"token_type\": \"Bearer\", \"expires_in\": \"1\"}")))
        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token2")
    }
}
