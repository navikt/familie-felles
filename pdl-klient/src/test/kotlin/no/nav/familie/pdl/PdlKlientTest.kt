package no.nav.familie.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.ExpectedCount.twice
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.time.LocalDate

class PdlKlientTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val klient = PdlMaskinKlient(URI.create(PDL_URL), builder.build(), behandlingsnummer = BEHANDLINGSNUMMER)

    @AfterEach
    fun verifiserKall() {
        server.verify()
    }

    @Test
    fun `sender graphql-post og deserialiserer konkret dto med begge kallestiler`() {
        // Arrange
        val graphql = "query HentPerson { ping }"
        val variabler = mapOf("ident" to "ident")
        val forventetJson =
            """
            {
              "query": "query HentPerson { ping }",
              "variables": {"ident":"ident"}
            }
            """.trimIndent()

        server
            .expect(twice(), requestTo(GRAPHQL_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header("Behandlingsnummer", BEHANDLINGSNUMMER))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(forventetJson, JsonCompareMode.STRICT))
            .andRespond { mockRespons("""{"data":{"hentPerson":{"navn":"navn"}}}""") }

        // Act
        val medTypeargument = klient.hent<TestSvarMedPerson>(graphql, variabler)
        val medTypeinferens: TestSvarMedPerson = klient.hent(graphql, variabler)

        // Assert
        assertThat(medTypeargument).isEqualTo(TestSvarMedPerson(TestPerson("navn")))
        assertThat(medTypeinferens).isEqualTo(TestSvarMedPerson(TestPerson("navn")))
    }

    @ParameterizedTest
    @MethodSource("behandlingsnummerSomSendes")
    fun `behandlingsnummer fra konfigurasjon eller kall sendes til PDL`(
        konfigurertBehandlingsnummer: String?,
        behandlingsnummerIKallet: String?,
        forventetBehandlingsnummer: String,
    ) {
        val klientMedNummer =
            PdlMaskinKlient(URI.create(PDL_URL), builder.build(), behandlingsnummer = konfigurertBehandlingsnummer)
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andExpect(header("Behandlingsnummer", forventetBehandlingsnummer))
            .andRespond { mockRespons("""{"data":"svar"}""") }

        val svar =
            if (behandlingsnummerIKallet == null) {
                klientMedNummer.hent<String>(GRAPHQL, emptyMap())
            } else {
                klientMedNummer.hent<String>(GRAPHQL, emptyMap(), behandlingsnummerIKallet)
            }

        assertThat(svar).isEqualTo("svar")
    }

    @Test
    fun `manglende behandlingsnummer i både konfigurasjon og kall gir IllegalStateException`() {
        val klientUtenNummer = PdlMaskinKlient(URI.create(PDL_URL), builder.build())

        val feil = assertThrows<IllegalStateException> { klientUtenNummer.hent<String>(GRAPHQL, emptyMap()) }

        assertThat(feil).hasMessageContaining("familie.felles.klient.pdl.behandlingsnummer")
    }

    @ParameterizedTest
    @MethodSource("blankeBehandlingsnummer")
    fun `blankt behandlingsnummer gir IllegalArgumentException`(
        konfigurertBehandlingsnummer: String?,
        behandlingsnummerIKallet: String?,
    ) {
        val klientMedNummer =
            PdlMaskinKlient(URI.create(PDL_URL), builder.build(), behandlingsnummer = konfigurertBehandlingsnummer)

        val feil =
            assertThrows<IllegalArgumentException> {
                if (behandlingsnummerIKallet == null) {
                    klientMedNummer.hent<String>(GRAPHQL, emptyMap())
                } else {
                    klientMedNummer.hent<String>(GRAPHQL, emptyMap(), behandlingsnummerIKallet)
                }
            }

        assertThat(feil).hasMessage("Behandlingsnummer kan ikke være blankt")
    }

    @Test
    fun `bevarer listetype og nestet generisk map-listetype`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("""{"data":[{"navn":"navn"}]}""") }

        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("""{"data":{"gruppe":[{"navn":"navn"}]}}""") }

        // Act
        val liste: List<TestPerson> = klient.hent(GRAPHQL, emptyMap())
        val nestet: Map<String, List<TestPerson>> = klient.hent(GRAPHQL, emptyMap())

        // Assert
        assertThat(liste).containsExactly(TestPerson("navn"))
        assertThat(nestet.getValue("gruppe").single()).isEqualTo(TestPerson("navn"))
    }

    @Test
    fun `gyldig data kan inneholde null nestet fagfelt`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("""{"data":{"hentPerson":null}}""") }

        // Act
        val svar: TestSvarMedPerson = klient.hent(GRAPHQL, emptyMap())

        // Assert
        assertThat(svar).isEqualTo(TestSvarMedPerson(null))
    }

    @Test
    fun `leser advarsler fra extensions`() {
        // Arrange
        val json =
            """
            {
              "data": "svar",
              "extensions": {
                "warnings": [
                  {
                    "query": "hentPerson",
                    "id": "tilgangsstyring",
                    "code": "mangler_opplysninger_i_katalog",
                    "message": "Behandling mangler opplysningstyper",
                    "details": {"missing": ["FALSK_ID_V1"]}
                  }
                ]
              }
            }
            """.trimIndent()

        // Act
        val respons = jacksonObjectMapper().readValue<PdlRespons<String>>(json)

        // Assert
        assertThat(respons.extensions?.warnings)
            .containsExactly(
                PdlAdvarsel(
                    query = "hentPerson",
                    id = "tilgangsstyring",
                    code = "mangler_opplysninger_i_katalog",
                    message = "Behandling mangler opplysningstyper",
                    details = mapOf("missing" to listOf("FALSK_ID_V1")),
                ),
            )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"data":"svar"}""",
            """{"data":"svar","extensions":null}""",
            """{"data":"svar","extensions":{}}""",
            """{"data":"svar","extensions":{"warnings":null}}""",
            """{"data":"svar","extensions":{"warnings":[{"code":"ukjent","details":"tekst"}]}}""",
            """{"data":"svar","extensions":{"warnings":[{"details":null,"fremtidig":"felt"}],"annet":"felt"}}""",
        ],
    )
    fun `svar med eller uten advarsler gir data`(body: String) {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        // Act
        val svar = klient.hent<String>(GRAPHQL, emptyMap())

        // Assert
        assertThat(svar).isEqualTo("svar")
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://pdl.test", "https://pdl.test/"])
    fun `graphqlUrl er url-en med graphql på slutten`(url: String) {
        // Arrange
        val klient = PdlMaskinKlient(URI.create(url), builder.build(), behandlingsnummer = BEHANDLINGSNUMMER)

        // Act
        val graphqlUrl = klient.graphqlUrl

        // Assert
        assertThat(graphqlUrl).isEqualTo(URI.create("https://pdl.test/graphql"))
    }

    @Test
    fun `graphql request skjuler verdier i toString`() {
        // Arrange
        val sensitivVerdi = "hemmelig"
        val request = PdlRequest(sensitivVerdi, mapOf("ident" to sensitivVerdi))

        // Act
        val tekst = request.toString()

        // Assert
        assertThat(tekst).doesNotContain(sensitivVerdi)
    }

    @ParameterizedTest
    @MethodSource("graphqlFeilMedOgUtenData")
    fun `graphql errors og partialdata bevarer PDL-meldinger`(
        body: String,
        forventetMelding: String,
    ) {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil).isExactlyInstanceOf(PdlException::class.java)
        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.feilmelding).contains("Svar fra PDL inneholder feil", forventetMelding)
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.cause).isNull()
        assertThat(feil.suppressed).isEmpty()
        assertThat(feil.stackTraceToString()).doesNotContain("persondata")
    }

    @ParameterizedTest
    @MethodSource("feilkoder")
    fun `bevarer feilkoder og feil uten klassifisering`(
        body: String,
        forventedeFeil: List<PdlFeil>,
    ) {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil).isExactlyInstanceOf(PdlException::class.java)
        assertThat(feil.errors).containsExactlyElementsOf(forventedeFeil)
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.cause).isNull()
    }

    @Test
    fun `bevarer PDL-feilklassifisering og dokumenterte extensions i exception`() {
        // Arrange
        server.expect(once(), requestTo(GRAPHQL_URL)).andRespond {
            mockRespons(
                """
                {
                  "errors": [
                    {
                      "message": "Ikke tilgang",
                      "locations": [{"line": 2, "column": 7}],
                      "path": ["hentPerson", "adresse"],
                      "extensions": {
                        "code": "unauthorized",
                        "details": {
                          "type": "abac-deny",
                          "cause": "cause-0001-manglerrolle",
                          "policy": "adressebeskyttelse_strengt_fortrolig_adresse"
                        },
                        "classification": "ExecutionAborted"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            )
        }

        val forventetFeil =
            PdlFeil(
                message = "Ikke tilgang",
                extensions =
                    PdlFeilExtensions(
                        code = "unauthorized",
                        details =
                            mapOf(
                                "type" to "abac-deny",
                                "cause" to "cause-0001-manglerrolle",
                                "policy" to "adressebeskyttelse_strengt_fortrolig_adresse",
                            ),
                        classification = "ExecutionAborted",
                    ),
                locations = listOf(mapOf("line" to 2, "column" to 7)),
                path = listOf("hentPerson", "adresse"),
            )

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.errors).containsExactly(forventetFeil)
    }

    @Test
    fun `bevarer nestet struktur i details og ukjent klassifisering`() {
        // Arrange
        server.expect(once(), requestTo(GRAPHQL_URL)).andRespond {
            mockRespons(
                """
                {
                  "errors": [
                    {
                      "message": "Nested metadata",
                      "extensions": {
                        "details": {
                          "nested": {"items": ["tekst", true, null], "enabled": false},
                          "nothing": null
                        },
                        "classification": "FutureClassification"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            )
        }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        val extensions = feil.errors.single().extensions
        assertThat(extensions?.details)
            .isEqualTo(
                mapOf(
                    "nested" to mapOf("items" to listOf("tekst", true, null), "enabled" to false),
                    "nothing" to null,
                ),
            )
        assertThat(extensions?.classification).isEqualTo("FutureClassification")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"errors":[{"message":"Missing metadata","extensions":{}}]}""",
            """{"errors":[{"message":"Null metadata","extensions":{"details":null,"classification":null}}]}""",
        ],
    )
    fun `manglende eller null details og klassifisering blir null`(body: String) {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        val extensions = feil.errors.single().extensions
        assertThat(extensions?.details).isNull()
        assertThat(extensions?.classification).isNull()
    }

    @Test
    fun `feil-dto tolererer ukjente felt og bevarer graphql-metadata`() {
        // Arrange
        val body =
            """
            {
              "data": "persondata",
              "errors": [
                {
                  "message": "Fant ikke person",
                  "locations": [{"line": 2, "column": 7}],
                  "path": ["hentPerson", "addresses", 0, "street"],
                  "details": "detaljer",
                  "extensions": {"code": "not_found", "future": "fremtidig"}
                }
              ],
              "futureResponse": "respons"
            }
            """.trimIndent()

        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        val forventetFeil =
            PdlFeil(
                message = "Fant ikke person",
                extensions = PdlFeilExtensions(code = "not_found"),
                locations = listOf(mapOf("line" to 2, "column" to 7)),
                path = listOf("hentPerson", "addresses", 0, "street"),
            )

        val synligeVerdier =
            arrayOf("Fant ikke person", "not_found", "line=2", "column=7", "hentPerson", "addresses", "0", "street")

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil).isExactlyInstanceOf(PdlException::class.java)
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.errors).containsExactly(forventetFeil)
        assertThat(feil.errors.single().toString()).contains(*synligeVerdier)
        assertThat(
            feil.errors
                .single()
                .extensions
                .toString(),
        ).contains("not_found")
        assertThat(feil.errors.toString()).contains(*synligeVerdier)
        arrayOf(
            "detaljer",
            "fremtidig",
            "respons",
            "persondata",
        ).filter(String::isNotEmpty)
            .forEach { assertThat(feil.errors.toString()).doesNotContain(it) }
        arrayOf(body, "persondata", "hentPerson", "not_found")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.feilmelding).doesNotContain(it) }
        arrayOf("persondata")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.toString()).doesNotContain(it) }
        arrayOf(body, "persondata")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.stackTraceToString()).doesNotContain(it) }
    }

    @Test
    fun `graphql-feil uten metadata har nullable locations og path`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("""{"errors":[{"message":"melding"},{"message":"annen melding","locations":null,"path":null}]}""") }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.errors)
            .containsExactly(
                PdlFeil(message = "melding"),
                PdlFeil(message = "annen melding", locations = null, path = null),
            )
        feil.errors.forEach {
            assertThat(it.locations).isNull()
            assertThat(it.path).isNull()
        }
    }

    @Test
    fun `tomme PDL-meldinger feiler med standardtekst`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("""{"errors":[{"message":null},{"message":""},{"message":"   "}]}""") }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.feilmelding).isEqualTo("Svar fra PDL inneholder feil")
    }

    @Test
    fun `flere PDL-meldinger beholdes i samme feilmelding`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("""{"errors":[{"message":"Fant ikke person"},{"message":"Ikke tilgang"}]}""") }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.feilmelding).isEqualTo("Svar fra PDL inneholder feil: Fant ikke person; Ikke tilgang")
    }

    @Test
    fun `PDL-meldinger beholder kontrolltegn uten å eksponere responsdetaljer`() {
        // Arrange
        val body =
            """
            {
              "data": "persondata",
              "errors": [
                {
                  "message": "Fant\nikke\tperson\u2028klar",
                  "path": ["hentPerson"],
                  "extensions": {
                    "code": "kode",
                    "details": {"type": "detaljer"}
                  }
                }
              ]
            }
            """.trimIndent()

        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.feilmelding).isEqualTo("Svar fra PDL inneholder feil: Fant\nikke\tperson\u2028klar")
        assertThat(
            feil.errors
                .single()
                .extensions
                ?.details,
        ).isEqualTo(mapOf("type" to "detaljer"))
        assertThat(feil.cause).isNull()
        assertThat(feil.suppressed).isEmpty()
        arrayOf("persondata")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.toString()).doesNotContain(it) }
        arrayOf(body)
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.message.orEmpty()).doesNotContain(it) }
        arrayOf("hentPerson", "kode", "detaljer", "persondata")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.feilmelding).doesNotContain(it) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}", """{"data":null}"""])
    fun `mangler toppdata gir en klientfeil`(body: String) {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL mangler data")
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.cause).isNull()
    }

    @Test
    fun `mangler body gir en klientfeil`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("", HttpStatus.NO_CONTENT, contentType = null) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL mangler body")
        assertThat(feil.httpStatus).isNull()
    }

    @ParameterizedTest
    @MethodSource("httpFeilsvar")
    fun `HTTP-feil beholder status og body i cause uten graphql-feil`(
        body: String,
        status: Int,
        contentType: String?,
    ) {
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body, HttpStatusCode.valueOf(status), contentType) }

        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        assertThat(feil).isExactlyInstanceOf(PdlException::class.java)
        assertThat(feil.message).isEqualTo("Kall mot PDL feilet med HTTP-status $status")
        assertThat(feil.httpStatus).isEqualTo(status)
        assertThat(feil.errors).isEmpty()
        assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        val opprinneligFeil = feil.cause as RestClientResponseException
        assertThat(opprinneligFeil.statusCode.value()).isEqualTo(status)
        assertThat(opprinneligFeil.responseBodyAsString).isEqualTo(body)
        if (body.isNotEmpty()) assertThat(feil.toString()).doesNotContain(body)
    }

    @Test
    fun `HTTP-feil med IO-feil i body bruker fast melding med cause`() {
        // Arrange
        val respons = ResponsMedBodyfeil(HttpStatusCode.valueOf(403), IOException("io-feil"))
        respons.headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        server.expect(once(), requestTo(GRAPHQL_URL)).andRespond { respons }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Kall mot PDL feilet med HTTP-status 403")
        assertThat(feil.httpStatus).isEqualTo(403)
        assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        assertThat((feil.cause as RestClientResponseException).statusCode.value()).isEqualTo(403)
        arrayOf("io-feil")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.toString()).doesNotContain(it) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{",
            """{"data":"svar","errors":"ikke-en-liste"}""",
            """{"data":"svar","errors":[null]}""",
        ],
    )
    fun `ugyldig json og ugyldig errors-struktur gir lesefeil uten cause`(body: String) {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons(body) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).startsWith("Svar fra PDL kunne ikke leses")
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.cause).isNull()
        arrayOf(body, "svar", "ikke-en-liste")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.stackTraceToString()).doesNotContain(it) }
    }

    @Test
    fun `svar med ukjent innholdstype gir lesefeil uten cause`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("<html>fnr</html>", contentType = MediaType.TEXT_HTML_VALUE) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL kunne ikke leses")
        assertThat(feil.cause).isNull()
        arrayOf("fnr")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.stackTraceToString()).doesNotContain(it) }
    }

    @Test
    fun `data som ikke passer typen gir lesefeil med sti uten verdier`() {
        // Arrange
        server
            .expect(once(), requestTo(GRAPHQL_URL))
            .andRespond { mockRespons("""{"data":[{"navn":"navn","fødselsdato":"fnr"}]}""") }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<List<TestPerson>>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL kunne ikke leses: [data.[0].fødselsdato]")
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.cause).isNull()
        arrayOf("fnr")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.stackTraceToString()).doesNotContain(it) }
    }

    @Test
    fun `graphql-feil bevares når dto følger nullbarheten i PDL-skjemaet`() {
        // Arrange
        server.expect(once(), requestTo(GRAPHQL_URL)).andRespond {
            mockRespons(
                """
                {
                  "data": {"hentPerson": null},
                  "errors": [{"message": "Fant ikke person", "extensions": {"code": "not_found"}}]
                }
                """.trimIndent(),
            )
        }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<TestSvarMedPerson>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.errors).containsExactly(PdlFeil("Fant ikke person", PdlFeilExtensions("not_found")))
        assertThat(feil.cause).isNull()
    }

    @Test
    fun `påkrevd felt som PDL sender som null gir lesefeil uten verdier`() {
        // Arrange
        server.expect(once(), requestTo(GRAPHQL_URL)).andRespond {
            mockRespons(
                """
                {
                  "data": {"hentPerson": null, "annet": "fnr"},
                  "errors": [{"message": "Fant ikke person", "extensions": {"code": "not_found"}}]
                }
                """.trimIndent(),
            )
        }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<TestSvarMedPåkrevdPerson>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).startsWith("Svar fra PDL kunne ikke leses").contains("hentPerson")
        assertThat(feil.errors).isEmpty()
        assertThat(feil.cause).isNull()
        arrayOf("fnr")
            .filter(String::isNotEmpty)
            .forEach { assertThat(feil.stackTraceToString()).doesNotContain(it) }
    }

    @Test
    fun `transportfeil beholder eksakt originalfeil som cause`() {
        // Arrange
        val originalFeil = IllegalStateException("transportfeil")
        val restClient =
            RestClient
                .builder()
                .requestInterceptor { _, _, _ -> throw originalFeil }
                .build()
        val klientMedTransportfeil =
            PdlMaskinKlient(URI.create(PDL_URL), restClient, behandlingsnummer = BEHANDLINGSNUMMER)

        // Act
        val feil = assertThrows<PdlException> { klientMedTransportfeil.hent<String>(GRAPHQL, emptyMap()) }

        // Assert
        assertThat(feil.message).isEqualTo("Kall mot PDL feilet")
        assertThat(feil.cause).isSameAs(originalFeil)
    }

    @Test
    fun `Error fra interceptor kastes uendret gjennom hent`() {
        val originalFeil = AssertionError("testfeil")
        val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
        val klientMedFeil = PdlMaskinKlient(URI.create(PDL_URL), restClient, behandlingsnummer = BEHANDLINGSNUMMER)

        val feil = assertThrows<AssertionError> { klientMedFeil.hent<String>(GRAPHQL, emptyMap()) }

        assertThat(feil).isSameAs(originalFeil)
    }

    @Test
    fun `exception har enkel melding og status uten cause`() {
        // Arrange
        val melding = "Linje\r\nmed\tkontroll\u0001\u0001skift\u2028klar"
        val mutableErrors = mutableListOf(PdlFeil("melding"))
        val originalFeil = IllegalStateException("cause")

        // Act
        val standard = PdlException()
        val feil = PdlException(melding, httpStatus = 403)
        val kopi = PdlException(errors = mutableErrors)
        mutableErrors.clear()
        val medCause = PdlException("melding", mutableErrors, 500, originalFeil)

        // Assert
        assertThat(standard.message).isEqualTo("Kall mot PDL feilet")
        assertThat(standard.httpStatus).isNull()
        assertThat(feil.message).isEqualTo(melding)
        assertThat(feil.httpStatus).isEqualTo(403)
        assertThat(feil.cause).isNull()
        assertThat(feil.suppressed).isEmpty()
        assertThat(kopi.errors).isSameAs(mutableErrors)
        assertThat(kopi.errors).isEmpty()
        assertThat(medCause.cause).isSameAs(originalFeil)
    }

    private fun mockRespons(
        body: String,
        status: HttpStatusCode = HttpStatus.OK,
        contentType: String? = MediaType.APPLICATION_JSON_VALUE,
    ): MockClientHttpResponse =
        MockClientHttpResponse(body.toByteArray(), status).apply {
            contentType?.let { headers.set(HttpHeaders.CONTENT_TYPE, it) }
        }

    private class ResponsMedBodyfeil(
        status: HttpStatusCode,
        private val opprinneligIoFeil: IOException,
    ) : MockClientHttpResponse(ByteArray(0), status) {
        override fun getBody(): InputStream = throw opprinneligIoFeil

        override fun close() = Unit
    }

    private data class TestPerson(
        val navn: String,
        val fødselsdato: LocalDate? = null,
    )

    private data class TestSvarMedPerson(
        val hentPerson: TestPerson?,
    )

    private data class TestSvarMedPåkrevdPerson(
        val hentPerson: TestPerson,
    )

    companion object {
        private const val PDL_URL = "https://pdl.test"
        private const val GRAPHQL_URL = "$PDL_URL/graphql"
        private const val GRAPHQL = "query HentPerson { ping }"
        private const val BEHANDLINGSNUMMER = "B123"

        @JvmStatic
        fun graphqlFeilMedOgUtenData(): List<Arguments> =
            listOf(
                arguments("""{"errors":[{"message":"Fant ikke person"}]}""", "Fant ikke person"),
                arguments("""{"data":"persondata","errors":[{"message":"Ikke tilgang"}]}""", "Ikke tilgang"),
            )

        @JvmStatic
        fun behandlingsnummerSomSendes(): List<Arguments> =
            listOf(
                arguments(BEHANDLINGSNUMMER, "B999", "B999"),
                arguments(BEHANDLINGSNUMMER, null, BEHANDLINGSNUMMER),
                arguments(null, "B999", "B999"),
            )

        @JvmStatic
        fun blankeBehandlingsnummer(): List<Arguments> =
            listOf(
                arguments(BEHANDLINGSNUMMER, " "),
                arguments("", null),
            )

        @JvmStatic
        fun feilkoder(): List<Arguments> =
            listOf(
                arguments(
                    """{"errors":[{"message":"Fant ikke person","extensions":{"code":"not_found"}}]}""",
                    listOf(PdlFeil("Fant ikke person", PdlFeilExtensions("not_found"))),
                ),
                arguments(
                    """
                    {"errors":[
                      {"message":"Fant ikke person","extensions":{"code":"not_found"}},
                      {"message":"Fant ikke adresse","extensions":{"code":"not_found"}}
                    ]}
                    """.trimIndent(),
                    listOf(
                        PdlFeil("Fant ikke person", PdlFeilExtensions("not_found")),
                        PdlFeil("Fant ikke adresse", PdlFeilExtensions("not_found")),
                    ),
                ),
                arguments(
                    """
                    {"errors":[
                      {"message":"Mangler","extensions":{"code":"not_found"}},
                      {"message":"Serverfeil","extensions":{"code":"server_error"}},
                      {"message":"Ikke tilgang","extensions":{"code":"unauthorized"}}
                    ]}
                    """.trimIndent(),
                    listOf(
                        PdlFeil("Mangler", PdlFeilExtensions("not_found")),
                        PdlFeil("Serverfeil", PdlFeilExtensions("server_error")),
                        PdlFeil("Ikke tilgang", PdlFeilExtensions("unauthorized")),
                    ),
                ),
                arguments("""{"errors":[{"message":"Mangler"}]}""", listOf(PdlFeil("Mangler"))),
                arguments("""{"errors":[{"message":"Mangler","extensions":null}]}""", listOf(PdlFeil("Mangler"))),
                arguments(
                    """{"errors":[{"message":"Mangler","extensions":{"code":null}}]}""",
                    listOf(PdlFeil("Mangler", PdlFeilExtensions())),
                ),
                arguments(
                    """{"errors":[{"message":"Mangler","extensions":{"code":"future_code"}}]}""",
                    listOf(PdlFeil("Mangler", PdlFeilExtensions("future_code"))),
                ),
                arguments(
                    """{"errors":[{"message":"Mangler","extensions":{"code":"NOT_FOUND"}}]}""",
                    listOf(PdlFeil("Mangler", PdlFeilExtensions("NOT_FOUND"))),
                ),
            )

        @JvmStatic
        fun httpFeilsvar(): List<Arguments> =
            listOf(
                arguments(
                    """{"errors":[{"message":"http-feil","extensions":{"code":"not_found"}}]}""",
                    404,
                    MediaType.APPLICATION_JSON_VALUE,
                ),
                arguments("", 404, MediaType.APPLICATION_JSON_VALUE),
                *listOf(400, 401, 403, 404, 429, 500)
                    .map {
                        arguments(
                            """{"errors":[{"message":"Ikke tilgang","extensions":{"code":"unauthorized"}}]}""",
                            it,
                            MediaType.APPLICATION_JSON_VALUE,
                        )
                    }.toTypedArray(),
                arguments("<html>feilside</html>", 500, MediaType.TEXT_HTML_VALUE),
                arguments("", 500, MediaType.APPLICATION_JSON_VALUE),
                arguments("{", 500, MediaType.APPLICATION_JSON_VALUE),
            )
    }
}
