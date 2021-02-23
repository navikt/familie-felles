package no.nav.familie.http.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.getForEntity
import java.net.SocketTimeoutException

internal class RestTemplateBuilderBeanTest {

    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
    private val port = wireMockServer.port()
    private val restTemplate = RestTemplateBuilderBean()
            .restTemplateBuilder(NaisProxyCustomizer(200, 200, 200))
            .build()

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    internal fun `delay med 500 kaster exception`() {
        wireMockServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                        .willReturn(WireMock.aResponse().withStatus(200).withFixedDelay(500)))
        assertThat(catchThrowable { restTemplate.getForEntity<String>("http://localhost:$port") })
                .hasCauseInstanceOf(SocketTimeoutException::class.java)
    }

    @Test
    internal fun `delay med 100 kaster ikke exception`() {
        wireMockServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                        .willReturn(WireMock.aResponse().withFixedDelay(100)))
        restTemplate.getForEntity<String>("http://localhost:$port")
    }
}
