package no.nav.familie.http.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.net.SocketTimeoutException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RestTemplateBuilderBeanTest {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var restTemplate: RestTemplate

    @BeforeAll
    fun setupServer() {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer.start()
    }

    @AfterAll
    fun tearDownServer() {
        wireMockServer.stop()
    }

    @BeforeEach
    fun setupRestTemplate() {
        // Sett timeouts direkte, SB4-kompatibelt
        val requestFactory =
            HttpComponentsClientHttpRequestFactory().apply {
                setConnectionRequestTimeout(400)
                setReadTimeout(400)
            }

        restTemplate =
            RestTemplateBuilder()
                .requestFactory { requestFactory }
                .build()
    }

    @AfterEach
    fun resetWireMock() {
        wireMockServer.resetAll()
    }

    @Test
    fun `delay med 500 kaster exception`() {
        wireMockServer.stubFor(
            WireMock
                .get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(200).withFixedDelay(500)),
        )

        val thrown =
            catchThrowable {
                restTemplate.getForEntity<String>("http://localhost:${wireMockServer.port()}")
            }

        assertThat(thrown).hasCauseInstanceOf(SocketTimeoutException::class.java)
    }

    @Test
    fun `delay med 50 kaster ikke exception`() {
        wireMockServer.stubFor(
            WireMock
                .get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withFixedDelay(50)),
        )

        // Kaster ikke exception
        restTemplate.getForEntity<String>("http://localhost:${wireMockServer.port()}")
    }
}
