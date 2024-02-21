package no.nav.familie.http.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.SocketTimeoutException

@Disabled("TODO trenger å fikse NaisProxyCustomizer først")
internal class RestTemplateBuilderBeanTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer
        private lateinit var restClient: RestClient

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
        restClient =
            RestTemplateBuilderBean().restTemplateBuilderNoProxy().build()
    }

    @Test
    internal fun `delay med 500 kaster exception`() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(200).withFixedDelay(500)),
        )
        assertThat(
            catchThrowable {
                restClient.get().uri("http://localhost:${wireMockServer.port()}").retrieve().body<String>()
            },
        )
            .hasCauseInstanceOf(SocketTimeoutException::class.java)
    }

    @Test
    internal fun `delay med 50 kaster ikke exception`() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withFixedDelay(50)),
        )
        restClient.get().uri("http://localhost:${wireMockServer.port()}").retrieve().body<String>()
    }
}
