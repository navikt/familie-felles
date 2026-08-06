package no.nav.familie.tilgangsmaskin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import java.net.URI

class TilgangsmaskinKlientTest {
    private lateinit var wiremockServer: WireMockServer
    private lateinit var tilgangsmaskinKlient: TilgangsmaskinKlient

    @BeforeEach
    fun setUp() {
        wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
        wiremockServer.start()
        tilgangsmaskinKlient =
            TilgangsmaskinKlient(
                tilgangsmaskinUri = URI.create(wiremockServer.baseUrl()),
                restClient = RestClient.builder().build(),
            )
    }

    @AfterEach
    fun tearDown() {
        wiremockServer.stop()
    }

    @Test
    fun `skal gi tilgang når Tilgangsmaskinen svarer 204 for personen`() {
        // Arrange
        stubBulk("""{ "ansattId": "Z999999", "resultater": [ { "brukerId": "12345678910", "status": 204 } ] }""")

        // Act
        val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910"))

        // Assert
        assertThat(resultater).hasSize(1)
        assertThat(resultater.single().harTilgang).isTrue()
        assertThat(resultater.single().avvisningskode).isNull()
    }

    @Test
    fun `skal beholde avvisningskode, begrunnelse, traceId og kanOverstyres ved avvisning`() {
        // Arrange
        stubBulk(
            """
            { "ansattId": "Z999999",
              "resultater": [
                { "brukerId": "12345678910", "status": 403,
                  "detaljer": {
                    "title": "AVVIST_GEOGRAFISK",
                    "begrunnelse": "Bruker tilhører en annen enhet",
                    "traceId": "en-trace-id",
                    "kanOverstyres": true
                  }
                }
              ] }
            """.trimIndent(),
        )

        // Act
        val resultat = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910")).single()

        // Assert
        assertThat(resultat.harTilgang).isFalse()
        assertThat(resultat.avvisningskode).isEqualTo(Avvisningskode.AVVIST_GEOGRAFISK)
        assertThat(resultat.begrunnelse).isEqualTo("Bruker tilhører en annen enhet")
        assertThat(resultat.traceId).isEqualTo("en-trace-id")
        assertThat(resultat.kanOverstyres).isTrue()
    }

    @Test
    fun `skal tolke ukjent avvisningskode som UKJENT i stedet for å feile`() {
        // Arrange
        stubBulk(
            """
            { "ansattId": "Z999999",
              "resultater": [
                { "brukerId": "12345678910", "status": 403,
                  "detaljer": { "title": "EN_HELT_NY_KODE", "begrunnelse": "Ny regel", "ukjentFelt": "verdi" } }
              ] }
            """.trimIndent(),
        )

        // Act
        val resultat = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910")).single()

        // Assert
        assertThat(resultat.harTilgang).isFalse()
        assertThat(resultat.avvisningskode).isEqualTo(Avvisningskode.UKJENT)
        assertThat(resultat.begrunnelse).isEqualTo("Ny regel")
    }

    @Test
    fun `skal nekte tilgang for ident som Tilgangsmaskinen ikke svarer for`() {
        // Arrange
        stubBulk("""{ "ansattId": "Z999999", "resultater": [ { "brukerId": "11111111111", "status": 204 } ] }""")

        // Act
        val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("11111111111", "22222222222"))

        // Assert
        assertThat(resultater).hasSize(2)
        assertThat(resultater.single { it.personIdent == "11111111111" }.harTilgang).isTrue()
        assertThat(resultater.single { it.personIdent == "22222222222" }.harTilgang).isFalse()
        assertThat(resultater.single { it.personIdent == "22222222222" }.begrunnelse)
            .isEqualTo("Fikk ikke svar fra Tilgangsmaskinen for personen")
    }

    @Test
    fun `skal returnere ett resultat per etterspurt ident også når samme ident sendes inn flere ganger`() {
        // Arrange
        stubBulk("""{ "ansattId": "Z999999", "resultater": [ { "brukerId": "12345678910", "status": 204 } ] }""")

        // Act
        val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910", "12345678910"))

        // Assert
        assertThat(resultater).hasSize(2)
        assertThat(resultater).allMatch { it.harTilgang }
    }

    @Test
    fun `skal dele opp i flere kall når det er flere enn maks antall identer`() {
        // Arrange
        val identer = (1..TilgangsmaskinKlient.MAKS_ANTALL_IDENTER_PER_KALL + 1).map { it.toString().padStart(11, '0') }
        stubBulk("""{ "ansattId": "Z999999", "resultater": [] }""")

        // Act
        tilgangsmaskinKlient.sjekkTilgangTilPersoner(identer)

        // Assert
        wiremockServer.verify(2, postRequestedFor(urlEqualTo(BULK_PATH)))
    }

    @Test
    fun `skal ikke kalle Tilgangsmaskinen når det ikke er noen identer å sjekke`() {
        // Act
        val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(emptyList())

        // Assert
        assertThat(resultater).isEmpty()
        wiremockServer.verify(0, postRequestedFor(urlEqualTo(BULK_PATH)))
    }

    @Test
    fun `skal kaste TilgangsmaskinException når Tilgangsmaskinen feiler`() {
        // Arrange
        wiremockServer.stubFor(post(urlEqualTo(BULK_PATH)).willReturn(aResponse().withStatus(500)))

        // Act & Assert
        assertThatThrownBy { tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910")) }
            .isInstanceOf(TilgangsmaskinException::class.java)
    }

    @Test
    fun `skal nekte tilgang for ukjent person som Tilgangsmaskinen svarer 404 for`() {
        // Arrange
        // Tilgangsmaskinen har en egen kategori for ukjente personer (AggregertBulkRespons.ukjente = 404).
        stubBulk("""{ "resultater": [ { "brukerId": "12345678910", "status": 404 } ] }""")

        // Act
        val resultat = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910")).single()

        // Assert
        assertThat(resultat.harTilgang).isFalse()
        assertThat(resultat.httpStatus).isEqualTo(404)
        assertThat(resultat.avvisningskode).isEqualTo(Avvisningskode.UKJENT)
    }

    @Test
    fun `skal takle avvisning uten title uten å feile`() {
        // Arrange
        // title er nullable uten defaultverdi. Fravær skal gi null (og dermed UKJENT), ikke deserialiseringsfeil.
        stubBulk(
            """{ "resultater": [ { "brukerId": "12345678910", "status": 403, "detaljer": { "begrunnelse": "Ingen kode" } } ] }""",
        )

        // Act
        val resultat = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910")).single()

        // Assert
        assertThat(resultat.harTilgang).isFalse()
        assertThat(resultat.avvisningskode).isEqualTo(Avvisningskode.UKJENT)
        assertThat(resultat.begrunnelse).isEqualTo("Ingen kode")
    }

    @Test
    fun `skal ikke la en tom ident velte tilgangssjekken for de gyldige identene`() {
        // Arrange
        // Tilgangsmaskinen avviser hele forespørselen med 400 dersom én brukerId er tom.
        stubBulk("""{ "resultater": [ { "brukerId": "12345678910", "status": 204 } ] }""")

        // Act
        val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910", "  "))

        // Assert
        assertThat(resultater).hasSize(2)
        assertThat(resultater.first().harTilgang).isTrue()
        assertThat(resultater.last().harTilgang).isFalse()
        assertThat(resultater.last().httpStatus).isEqualTo(400)
        wiremockServer.verify(
            postRequestedFor(urlEqualTo(BULK_PATH))
                .withRequestBody(equalToJson("""[ { "brukerId": "12345678910", "type": "KJERNE_REGELTYPE" } ]""")),
        )
    }

    @Test
    fun `skal ikke kalle Tilgangsmaskinen når alle identene er tomme`() {
        // Act
        val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf(""))

        // Assert
        assertThat(resultater).hasSize(1)
        assertThat(resultater.single().harTilgang).isFalse()
        wiremockServer.verify(0, postRequestedFor(urlEqualTo(BULK_PATH)))
    }

    @Test
    fun `skal sende brukerId og regeltype i forespørselen`() {
        // Arrange
        stubBulk("""{ "ansattId": "Z999999", "resultater": [ { "brukerId": "12345678910", "status": 204 } ] }""")

        // Act
        tilgangsmaskinKlient.sjekkTilgangTilPersoner(listOf("12345678910"), Regeltype.KOMPLETT_REGELTYPE)

        // Assert
        wiremockServer.verify(
            postRequestedFor(urlEqualTo(BULK_PATH))
                .withRequestBody(
                    equalToJson("""[ { "brukerId": "12345678910", "type": "KOMPLETT_REGELTYPE" } ]"""),
                ),
        )
    }

    @Test
    fun `skal slå sammen resultater fra alle chunkene`() {
        // Arrange
        val forsteChunk = (1..TilgangsmaskinKlient.MAKS_ANTALL_IDENTER_PER_KALL).map { it.toString().padStart(11, '0') }
        val sisteIdent = "99999999999"
        wiremockServer.stubFor(
            post(urlEqualTo(BULK_PATH))
                .withRequestBody(containing(sisteIdent))
                .willReturn(bulkRespons("""{ "resultater": [ { "brukerId": "$sisteIdent", "status": 204 } ] }""")),
        )
        wiremockServer.stubFor(
            post(urlEqualTo(BULK_PATH))
                .withRequestBody(containing(forsteChunk.first()))
                .willReturn(
                    bulkRespons("""{ "resultater": [ { "brukerId": "${forsteChunk.first()}", "status": 204 } ] }"""),
                ),
        )

        // Act
        val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(forsteChunk + sisteIdent)

        // Assert
        wiremockServer.verify(2, postRequestedFor(urlEqualTo(BULK_PATH)))
        assertThat(resultater.single { it.personIdent == forsteChunk.first() }.harTilgang).isTrue()
        assertThat(resultater.single { it.personIdent == sisteIdent }.harTilgang).isTrue()
    }

    @Test
    fun `skal sende ett kall når det er nøyaktig maks antall identer`() {
        // Arrange
        val identer = (1..TilgangsmaskinKlient.MAKS_ANTALL_IDENTER_PER_KALL).map { it.toString().padStart(11, '0') }
        stubBulk("""{ "resultater": [] }""")

        // Act
        tilgangsmaskinKlient.sjekkTilgangTilPersoner(identer)

        // Assert
        wiremockServer.verify(1, postRequestedFor(urlEqualTo(BULK_PATH)))
    }

    @Test
    fun `skal feile høylytt i stedet for stille dersom Kotlin-modulen mangler på ObjectMapperen`() {
        // Arrange
        // Har alle felt i en data class defaultverdi, lager Kotlin en syntetisk no-arg-konstruktør som Jackson
        // foretrekker uten Kotlin-modulen. Da ville vi fått tomme felter uten feilmelding. Denne testen låser
        // at DTO-ene ikke har den fellen.
        val mapperUtenKotlinModul = JsonMapper.builder().build()
        val json = """{ "ansattId": "Z999999", "resultater": [ { "brukerId": "12345678910", "status": 204 } ] }"""

        // Act & Assert
        assertThatThrownBy { mapperUtenKotlinModul.readValue(json, TilgangsmaskinBulkResponsDto::class.java) }
            .isInstanceOf(JacksonException::class.java)
    }

    private fun stubBulk(respons: String) {
        wiremockServer.stubFor(post(urlEqualTo(BULK_PATH)).willReturn(bulkRespons(respons)))
    }

    // Tilgangsmaskinen svarer 207 MULTI_STATUS paa bulk-endepunktet, ikke 200.
    private fun bulkRespons(respons: String): ResponseDefinitionBuilder =
        aResponse()
            .withStatus(HttpStatus.MULTI_STATUS.value())
            .withHeader("Content-Type", "application/json")
            .withBody(respons)

    companion object {
        private const val BULK_PATH = "/api/v1/bulk/obo"
    }
}
