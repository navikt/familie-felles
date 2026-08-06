package no.nav.familie.tilgangsmaskin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.web.client.RestClient

class TilgangsmaskinKlientConfigTest {
    private lateinit var wiremockServer: WireMockServer

    @BeforeEach
    fun setUp() {
        wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServer.start()
    }

    @AfterEach
    fun tearDown() {
        wiremockServer.stop()
    }

    @Test
    fun `skal lage klient med den kvalifiserte RestClienten og url fra property`() {
        // Arrange
        wiremockServer.stubFor(
            post(urlEqualTo("/api/v1/bulk/obo")).willReturn(
                aResponse()
                    .withStatus(207)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{ "resultater": [ { "brukerId": "12345678910", "status": 204 } ] }"""),
            ),
        )
        val kontekst = AnnotationConfigApplicationContext()
        kontekst.environment.propertySources.addFirst(
            MapPropertySource("test", mapOf("TILGANGSMASKIN_API_URL" to wiremockServer.baseUrl())),
        )
        kontekst.register(RestClientTestConfig::class.java, TilgangsmaskinKlientConfig::class.java)
        kontekst.refresh()

        // Act
        val klient = kontekst.getBean(TilgangsmaskinKlient::class.java)
        val resultater = klient.sjekkTilgangTilPersoner(listOf("12345678910"))

        // Assert
        assertThat(resultater.single().harTilgang).isTrue()
        wiremockServer.verify(
            postRequestedFor(urlEqualTo("/api/v1/bulk/obo"))
                .withHeader(KLIENT_HEADER, equalTo("tilgangsmaskin"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json")),
        )
        kontekst.close()
    }

    @Configuration
    class RestClientTestConfig {
        @Bean("tilgangsmaskinRestClient")
        fun tilgangsmaskinRestClient(): RestClient =
            RestClient
                .builder()
                .defaultHeader(KLIENT_HEADER, "tilgangsmaskin")
                .defaultHeader("Accept", "text/plain")
                .build()

        // Finnes kun for at konteksten skal ha flere RestClient-bønner, slik testen speiler konsumentene.
        // Uten den ville Spring injisert på type alene, og testen vært grønn selv om @Qualifier manglet.
        @Bean("restClientSomIkkeSkalVelges")
        fun restClientSomIkkeSkalVelges(): RestClient = RestClient.builder().defaultHeader(KLIENT_HEADER, "skal-ikke-velges").build()
    }

    companion object {
        private const val KLIENT_HEADER = "X-Test-Klient"
    }
}
