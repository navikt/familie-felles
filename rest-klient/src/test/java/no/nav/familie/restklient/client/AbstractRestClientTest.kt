package no.nav.familie.restklient.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.restklient.client.AbstractRestClient
import no.nav.familie.restklient.client.RessursException
import no.nav.familie.restklient.config.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

internal class AbstractRestClientTest {
    class TestClient(
        val uri: URI,
    ) : AbstractRestClient(RestTemplate(), "") {
        fun test() {
            getForEntity<Ressurs<Any>>(uri)
        }
    }

    companion object {
        private lateinit var wireMockServer: WireMockServer
        private lateinit var client: TestClient

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
        client = TestClient(URI.create("http://localhost:${wireMockServer.port()}"))
    }

    @Test
    internal fun `feil med ressurs kaster RessursException`() {
        val ressurs = Ressurs.failure<Any>("Feilet")
        val body = jsonMapper.writeValueAsString(ressurs)
        wireMockServer.stubFor(
            WireMock
                .get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(500).withBody(body)),
        )
        val catchThrowable = catchThrowable { client.test() }
        assertThat(catchThrowable).isInstanceOfAny(RessursException::class.java)
        assertThat(catchThrowable).hasCauseInstanceOf(HttpServerErrorException::class.java)
        val ressursException = catchThrowable as RessursException
        assertThat(ressursException.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(ressursException.ressurs.melding).isEqualTo(ressurs.melding)
    }

    @Test
    internal fun `feil med body som inneholder feltet status men ikke er en ressurs`() {
        val body = jsonMapper.writeValueAsString(mapOf("status" to "nei"))
        wireMockServer.stubFor(
            WireMock
                .get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(500).withBody(body)),
        )
        val catchThrowable = catchThrowable { client.test() }
        assertThat(catchThrowable).isInstanceOfAny(HttpServerErrorException::class.java)
        assertThat((catchThrowable as HttpServerErrorException).statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    internal fun `feil uten ressurs kaster videre spring exception`() {
        wireMockServer.stubFor(
            WireMock
                .get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(500)),
        )
        val catchThrowable = catchThrowable { client.test() }
        assertThat(catchThrowable).isInstanceOfAny(HttpServerErrorException::class.java)
        assertThat((catchThrowable as HttpServerErrorException).statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
