package no.nav.familie.felles.tokenklient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient

class TokenHenterTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun startServer() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun stopServer() {
            wireMockServer.stop()
        }
    }

    @AfterEach
    fun resetServer() {
        wireMockServer.resetAll()
    }

    @Test
    fun `returnerer token etter første vellykkede kall`() {
        svarMed(WireMock.okJson(tokenRespons()))

        val token = hentToken()

        assertThat(token).isEqualTo("access-token")
        verifiserAntallKall(1)
    }

    @Test
    fun `prøver på nytt etter 500 og returnerer token`() {
        svarMedSekvens(WireMock.serverError(), WireMock.okJson(tokenRespons()))

        val token = hentToken()

        assertThat(token).isEqualTo("access-token")
        verifiserAntallKall(2)
    }

    @Test
    fun `kaster siste serverfeil etter to kall`() {
        svarMed(WireMock.serviceUnavailable())

        assertThatThrownBy { hentToken() }
            .isInstanceOf(HttpServerErrorException.ServiceUnavailable::class.java)
        verifiserAntallKall(2)
    }

    @Test
    fun `prøver ikke på nytt etter 400`() {
        svarMed(WireMock.badRequest())

        assertThatThrownBy { hentToken() }
            .isInstanceOf(HttpClientErrorException.BadRequest::class.java)
        verifiserAntallKall(1)
    }

    @Test
    fun `prøver på nytt etter connection reset og returnerer token`() {
        svarMedSekvens(
            WireMock.serverError().withFault(Fault.CONNECTION_RESET_BY_PEER),
            WireMock.okJson(tokenRespons()),
        )

        val token = hentToken()

        assertThat(token).isEqualTo("access-token")
        verifiserAntallKall(2)
    }

    @Test
    fun `prøver ikke på nytt når responsen er tom`() {
        svarMed(WireMock.ok())

        assertThatThrownBy { hentToken() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Fikk ikke svar fra token-endpoint")
        verifiserAntallKall(1)
    }

    private fun hentToken(): String =
        TokenHenter.hentToken(
            restClient = RestClient.create(),
            tokenEndpoint = "${wireMockServer.baseUrl()}/token",
            body = mapOf("identity_provider" to "entra_id"),
        )

    private fun svarMed(response: com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder) {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/token")).willReturn(response))
    }

    private fun svarMedSekvens(vararg responses: com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder) {
        wireMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/token"))
                .willReturn(responses.first())
                .inScenario("tokenveksling")
                .whenScenarioStateIs("Started")
                .willSetStateTo("andre kall"),
        )
        wireMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/token"))
                .willReturn(responses.last())
                .inScenario("tokenveksling")
                .whenScenarioStateIs("andre kall"),
        )
    }

    private fun verifiserAntallKall(antall: Int) {
        wireMockServer.verify(antall, WireMock.postRequestedFor(WireMock.urlEqualTo("/token")))
    }

    private fun tokenRespons() = """{"access_token":"access-token","expires_in":3600,"token_type":"Bearer"}"""
}
