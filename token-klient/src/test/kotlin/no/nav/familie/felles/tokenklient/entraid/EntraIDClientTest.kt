package no.nav.familie.felles.tokenklient.entraid

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class EntraIDClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer =
                WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
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

    @Test
    fun `skal returnere access_token fra velykket respons`() {
        wireMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/token"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"access_token":"mitt-token","expires_in":3600,"token_type":"Bearer"}"""),
                ),
        )

        val client =
            EntraIDClient(
                tokenEndpoint = "http://localhost:${wireMockServer.port()}/token",
                tokenExchangeEndpoint = "http://localhost:${wireMockServer.port()}/token",
            )
        val token = client.hentMaskinTilMaskinToken("api://min-tjeneste/.default")

        Assertions.assertEquals("mitt-token", token)
    }

    @Test
    fun `skal sende identity_provider og target i request-body`() {
        val scope = "api://min-tjeneste/.default"

        wireMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/token"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"access_token":"token","expires_in":3600,"token_type":"Bearer"}"""),
                ),
        )

        val client =
            EntraIDClient(
                tokenEndpoint = "http://localhost:${wireMockServer.port()}/token",
                tokenExchangeEndpoint = "http://localhost:${wireMockServer.port()}/token",
            )
        client.hentMaskinTilMaskinToken(scope)

        wireMockServer.verify(
            WireMock
                .postRequestedFor(WireMock.urlEqualTo("/token"))
                .withRequestBody(WireMock.equalToJson("""{"identity_provider":"entra_id","target":"$scope"}""")),
        )
    }

    @Test
    fun `skal returnere obo access_token fra vellykket respons`() {
        wireMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/token"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"access_token":"obo-token","expires_in":3600,"token_type":"Bearer"}"""),
                ),
        )

        val client =
            EntraIDClient(
                tokenEndpoint = "http://localhost:${wireMockServer.port()}/token",
                tokenExchangeEndpoint = "http://localhost:${wireMockServer.port()}/token",
            )
        val token = client.hentOboToken("api://min-tjeneste/.default", "bruker-token")

        Assertions.assertEquals("obo-token", token)
    }

    @Test
    fun `skal sende identity_provider, target og user_token ved obo`() {
        val scope = "api://min-tjeneste/.default"
        val brukerToken = "mitt-bruker-token"

        wireMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/token"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"access_token":"token","expires_in":3600,"token_type":"Bearer"}"""),
                ),
        )

        val client =
            EntraIDClient(
                tokenEndpoint = "http://localhost:${wireMockServer.port()}/token",
                tokenExchangeEndpoint = "http://localhost:${wireMockServer.port()}/token",
            )
        client.hentOboToken(scope, brukerToken)

        wireMockServer.verify(
            WireMock
                .postRequestedFor(WireMock.urlEqualTo("/token"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """{"identity_provider":"entra_id","target":"$scope","user_token":"$brukerToken"}""",
                    ),
                ),
        )
    }
}
