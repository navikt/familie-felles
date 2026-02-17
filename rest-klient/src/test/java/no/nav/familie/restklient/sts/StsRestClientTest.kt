package no.nav.familie.restklient.sts

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class StsRestClientTest {
    companion object {
        private lateinit var stsRestClient: StsRestClient
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
            StsRestClient(jsonMapper, URI.create("http://localhost:${wireMockServer.port()}"), "username", "password")
    }

    @Test
    fun `skal først hente token fra sts, og så bruke cache på neste`() {
        wireMockServer.stubFor(
            post(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("{\"access_token\": \"token1\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\"}"),
                ),
        )

        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
        wireMockServer.resetAll()
        wireMockServer.stubFor(
            post(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("{\"access_token\": \"token2\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\"}"),
                ),
        )
        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
    }

    @Test
    fun `skal først hente token fra sts, og så hente ny fordi token er utløpt`() {
        wireMockServer.stubFor(
            post(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("{\"access_token\": \"token1\", \"token_type\": \"Bearer\", \"expires_in\": \"1\"}"),
                ),
        )

        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")
        Thread.sleep(1000)
        wireMockServer.resetAll()
        wireMockServer.stubFor(
            post(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("{\"access_token\": \"token2\", \"token_type\": \"Bearer\", \"expires_in\": \"1\"}"),
                ),
        )
        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token2")
    }
}
