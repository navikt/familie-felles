package no.nav.familie.webflux.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URI

internal class AbstractWebClientTest {

    class TestClient(val uri: URI) : AbstractWebClient(WebClient.create(), "") {

        fun test(): Ressurs<String> {
            return getForEntity(uri)
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
    internal fun `happy case`() {
        val responseRessurs = Ressurs.success("Ok Åæø")
        val body = objectMapper.writeValueAsString(responseRessurs)
        wireMockServer.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.okJson(body)))

        val ressurs = client.test()

        assertThat(ressurs).isEqualTo(responseRessurs)
    }

    @Test
    internal fun `feil med ressurs kaster RessursException`() {
        val ressurs = Ressurs.failure<Any>("FeiletÅæø")
        val body = objectMapper.writeValueAsString(ressurs)
        wireMockServer.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(500).withBody(body))
        )

        val catchThrowable = catchThrowable { client.test() }

        assertThat(catchThrowable).isInstanceOfAny(RessursException::class.java)
        assertThat(catchThrowable).hasCauseInstanceOf(WebClientResponseException.InternalServerError::class.java)
        val ressursException = catchThrowable as RessursException
        assertThat(ressursException.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(ressursException.ressurs.melding).isEqualTo(ressurs.melding)
    }

    @Test
    internal fun `feil med body som inneholder feltet status men ikke er en ressurs`() {
        val body = objectMapper.writeValueAsString(mapOf("status" to "nei"))
        wireMockServer.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(500).withBody(body))
        )

        val catchThrowable = catchThrowable { client.test() }

        assertThat(catchThrowable).isInstanceOfAny(WebClientResponseException.InternalServerError::class.java)
        assertThat((catchThrowable as WebClientResponseException.InternalServerError).statusCode)
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    internal fun `feil uten ressurs kaster videre spring exception`() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(500))
        )

        val catchThrowable = catchThrowable { client.test() }

        assertThat(catchThrowable).isInstanceOfAny(WebClientResponseException.InternalServerError::class.java)
        assertThat((catchThrowable as WebClientResponseException.InternalServerError).statusCode)
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
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
}
