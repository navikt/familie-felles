package no.nav.familie.felles.tokenklient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TexasClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
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
            post(urlEqualTo("/token"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"access_token":"mitt-token","expires_in":3600,"token_type":"Bearer"}"""),
                ),
        )

        val client = TexasClient("http://localhost:${wireMockServer.port()}/token")
        val token = client.hentMaskinTilMaskinToken("api://min-tjeneste/.default")

        assertEquals("mitt-token", token)
    }

    @Test
    fun `skal sende identity_provider og target i request-body`() {
        val scope = "api://min-tjeneste/.default"

        wireMockServer.stubFor(
            post(urlEqualTo("/token"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"access_token":"token","expires_in":3600,"token_type":"Bearer"}"""),
                ),
        )

        val client = TexasClient("http://localhost:${wireMockServer.port()}/token")
        client.hentMaskinTilMaskinToken(scope)

        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/token"))
                .withRequestBody(equalToJson("""{"identity_provider":"entra_id","target":"$scope"}""")),
        )
    }
}
