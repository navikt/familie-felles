package no.nav.familie.http.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.web.client.getForEntity
import java.net.SocketTimeoutException

internal class RestTemplateBuilderBeanTest {

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

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
    }

    private val restTemplate = RestTemplateBuilderBean()
            .restTemplateBuilder(NaisProxyCustomizer(200, 200, 200))
            .build()

    @Test
    internal fun `delay med 500 kaster exception`() {
        wireMockServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                        .willReturn(WireMock.aResponse().withStatus(200).withFixedDelay(500)))
        assertThat(catchThrowable { restTemplate.getForEntity<String>("http://localhost:${wireMockServer.port()}") })
                .hasCauseInstanceOf(SocketTimeoutException::class.java)
    }

    @Test
    internal fun `delay med 100 kaster ikke exception`() {
        wireMockServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                        .willReturn(WireMock.aResponse().withFixedDelay(100)))
        restTemplate.getForEntity<String>("http://localhost:${wireMockServer.port()}")
    }
}
