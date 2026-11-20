package no.nav.familie.pdl

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.time.LocalDate

class PdlKlientTest {
    @Test
    fun `sender graphql-post og deserialiserer konkret dto med begge kallestiler`() {
        val (restClient, server) = klientoppsett()
        val graphql = "query SyntetiskOperasjon { ping }"
        val variabler = mapOf("ident" to "syntetisk-id", "antall" to 2, "aktiv" to true)
        val forventetJson =
            """
            {
              "query": "query SyntetiskOperasjon { ping }",
              "variables": {"ident":"syntetisk-id","antall":2,"aktiv":true}
            }
            """.trimIndent()

        repeat(2) {
            server
                .expect(once(), requestTo("https://pdl.test/graphql"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(forventetJson, true))
                .andRespond(
                    withSuccess(
                        """{"data":{"verdi":"syntetisk-svar","person":{"navn":"syntetisk-navn"}}}""",
                        MediaType.APPLICATION_JSON,
                    ),
                )
        }

        val klient = PdlMaskinKlient(URI.create("https://pdl.test/"), restClient)
        assertThat(klient.hent<TestSvar>(graphql, variabler))
            .isEqualTo(TestSvar("syntetisk-svar", TestPerson("syntetisk-navn")))
        val svar: TestSvar = klient.hent(graphql, variabler)
        assertThat(svar).isEqualTo(TestSvar("syntetisk-svar", TestPerson("syntetisk-navn")))
        server.verify()
    }

    @Test
    fun `bevarer listetype og nestet generisk map-listetype`() {
        val (restClient, server) = klientoppsett()
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond(withSuccess("""{"data":[{"navn":"syntetisk-navn"}]}""", MediaType.APPLICATION_JSON))
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond(
                withSuccess(
                    """{"data":{"gruppe":[{"navn":"syntetisk-navn"}]}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
        val klient = PdlMaskinKlient(URI.create("https://pdl.test"), restClient)

        val liste: List<TestPerson> = klient.hent(GRAPHQL, emptyMap())
        val nestet: Map<String, List<TestPerson>> = klient.hent(GRAPHQL, emptyMap())

        assertThat(liste).containsExactly(TestPerson("syntetisk-navn"))
        assertThat(nestet.getValue("gruppe").single()).isEqualTo(TestPerson("syntetisk-navn"))
        server.verify()
    }

    @Test
    fun `gyldig data kan inneholde null nestet fagfelt`() {
        val (restClient, server) = klientoppsett()
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond(withSuccess("""{"data":{"hentPerson":null}}""", MediaType.APPLICATION_JSON))

        val svar: TestSvarMedPerson =
            PdlMaskinKlient(URI.create("https://pdl.test"), restClient).hent(GRAPHQL, emptyMap())

        assertThat(svar).isEqualTo(TestSvarMedPerson(null))
        server.verify()
    }

    @Test
    fun `graphql request skjuler verdier i toString`() {
        val sensitivVerdi = "syntetisk-sensitiv-verdi"

        assertThat(PdlRequest(sensitivVerdi, mapOf("ident" to sensitivVerdi)).toString()).doesNotContain(sensitivVerdi)
    }

    @Test
    fun `graphql errors og partialdata bevarer PDL-meldinger`() {
        listOf(
            """{"errors":[{"message":"Fant ikke person"}]}""" to "Fant ikke person",
            """{"data":"syntetisk-data","errors":[{"message":"Ikke tilgang"}]}""" to "Ikke tilgang",
        ).forEach { (body, message) ->
            val feil = assertFeil(body, null, listOf("syntetisk-data"))
            assertThat(feil)
                .isExactlyInstanceOf(PdlException::class.java)
            assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
            assertThat(feil.feilmelding).contains("Svar fra PDL inneholder feil", message)
            assertThat(feil.cause).isNull()
        }
    }

    @Test
    fun `bevarer feilkoder og feil uten klassifisering`() {
        listOf(
            """{"errors":[{"message":"Fant ikke person","extensions":{"code":"not_found"}}]}""" to
                listOf(PdlFeil("Fant ikke person", PdlFeilExtensions("not_found"))),
            """
            {"errors":[
              {"message":"Fant ikke person","extensions":{"code":"not_found"}},
              {"message":"Fant ikke adresse","extensions":{"code":"not_found"}}
            ]}
            """.trimIndent() to
                listOf(
                    PdlFeil("Fant ikke person", PdlFeilExtensions("not_found")),
                    PdlFeil("Fant ikke adresse", PdlFeilExtensions("not_found")),
                ),
            """
            {"errors":[
              {"message":"Mangler","extensions":{"code":"not_found"}},
              {"message":"Serverfeil","extensions":{"code":"server_error"}},
              {"message":"Ikke tilgang","extensions":{"code":"unauthorized"}}
            ]}
            """.trimIndent() to
                listOf(
                    PdlFeil("Mangler", PdlFeilExtensions("not_found")),
                    PdlFeil("Serverfeil", PdlFeilExtensions("server_error")),
                    PdlFeil("Ikke tilgang", PdlFeilExtensions("unauthorized")),
                ),
            """{"errors":[{"message":"Mangler"}]}""" to listOf(PdlFeil("Mangler")),
            """{"errors":[{"message":"Mangler","extensions":null}]}""" to listOf(PdlFeil("Mangler")),
            """{"errors":[{"message":"Mangler","extensions":{"code":null}}]}""" to
                listOf(PdlFeil("Mangler", PdlFeilExtensions())),
            """{"errors":[{"message":"Mangler","extensions":{"code":"future_code"}}]}""" to
                listOf(PdlFeil("Mangler", PdlFeilExtensions("future_code"))),
            """{"errors":[{"message":"Mangler","extensions":{"code":"NOT_FOUND"}}]}""" to
                listOf(PdlFeil("Mangler", PdlFeilExtensions("NOT_FOUND"))),
        ).forEach { (body, errors) ->
            val feil = assertFeil(body, null)
            assertThat(feil)
                .isExactlyInstanceOf(PdlException::class.java)
            assertThat(feil.errors).containsExactlyElementsOf(errors)
            assertThat(feil.httpStatus).isNull()
            assertThat(feil.cause).isNull()
        }
    }

    @Test
    fun `bevarer PDL-feilklassifisering og dokumenterte extensions i exception`() {
        val body =
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
            """.trimIndent()
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

        val feil = assertFeil(body, null)

        assertThat(feil.errors).containsExactly(forventetFeil)
    }

    @Test
    fun `bevarer nestet struktur i details og nullable eller ukjent klassifisering`() {
        val nestedBody =
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
            """.trimIndent()
        val nestedFeil = assertFeil(nestedBody, null).errors.single()

        assertThat(nestedFeil.extensions?.details)
            .isEqualTo(
                mapOf(
                    "nested" to mapOf("items" to listOf("tekst", true, null), "enabled" to false),
                    "nothing" to null,
                ),
            )
        assertThat(nestedFeil.extensions?.classification).isEqualTo("FutureClassification")

        listOf(
            """{"errors":[{"message":"Missing metadata","extensions":{}}]}""",
            """{"errors":[{"message":"Null metadata","extensions":{"details":null,"classification":null}}]}""",
        ).forEach { body ->
            val extensions = assertFeil(body, null).errors.single().extensions
            assertThat(extensions?.details).isNull()
            assertThat(extensions?.classification).isNull()
        }
    }

    @Test
    fun `HTTP error-body beholdes som cause uten parsing av graphql-errors`() {
        val body = """{"errors":[{"message":"syntetisk-http-feil","extensions":{"code":"not_found"}}]}"""
        val feil = assertHttpFeil(body, 404)

        assertThat(feil)
            .isExactlyInstanceOf(PdlException::class.java)
        assertThat(feil.message).isEqualTo("Kall mot PDL feilet med HTTP-status 404")
        assertThat(feil.httpStatus).isEqualTo(404)
        assertThat(feil.errors).isEmpty()
        assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        val opprinneligFeil = feil.cause as RestClientResponseException
        assertThat(opprinneligFeil.statusCode.value()).isEqualTo(404)
        assertThat(opprinneligFeil.responseBodyAsString).isEqualTo(body)

        val utenErrors = assertHttpFeil("", 404)
        assertThat(utenErrors)
            .isExactlyInstanceOf(PdlException::class.java)
        assertThat(utenErrors.errors).isEmpty()
    }

    @Test
    fun `feil-dto tolererer ukjente felt og bevarer graphql-metadata`() {
        val body =
            """
            {
              "data": "data-sentinel",
              "errors": [
                {
                  "message": "Fant ikke person",
                  "locations": [{"line": 2, "column": 7}],
                  "path": ["path-sentinel", "addresses", 0, "street"],
                  "details": "details-sentinel",
                  "extensions": {"code": "not_found", "future": "future-sentinel"}
                }
              ],
              "futureResponse": "response-sentinel"
            }
            """.trimIndent()
        val feil = assertFeil(body, null)
        val forventetFeil =
            PdlFeil(
                message = "Fant ikke person",
                extensions = PdlFeilExtensions(code = "not_found"),
                locations = listOf(mapOf("line" to 2, "column" to 7)),
                path = listOf("path-sentinel", "addresses", 0, "street"),
            )

        assertThat(feil)
            .isExactlyInstanceOf(PdlException::class.java)
        assertThat(feil.errors).containsExactly(forventetFeil)
        assertThat(feil.errors.single().locations).isEqualTo(listOf(mapOf("line" to 2, "column" to 7)))
        assertThat(feil.errors.single().path).isEqualTo(listOf("path-sentinel", "addresses", 0, "street"))
        assertThat(feil.errors.single().toString())
            .contains(
                "Fant ikke person",
                "not_found",
                "line=2",
                "column=7",
                "path-sentinel",
                "addresses",
                "0",
                "street",
            )
        assertThat(
            feil.errors
                .single()
                .extensions
                .toString(),
        ).contains("not_found")
        assertThat(feil.errors.toString())
            .contains(
                "Fant ikke person",
                "not_found",
                "line=2",
                "column=7",
                "path-sentinel",
                "addresses",
                "0",
                "street",
            ).doesNotContain("details-sentinel", "future-sentinel", "response-sentinel", "data-sentinel")
        assertThat(feil.feilmelding).doesNotContain(body, "data-sentinel", "path-sentinel", "not_found")
        assertThat(feil.toString()).doesNotContain("data-sentinel")
        assertThat(feil.stackTraceToString()).doesNotContain(body, "data-sentinel")
    }

    @Test
    fun `graphql-feil uten metadata har nullable locations og path`() {
        val feil =
            assertFeil(
                """
                {"errors":[{"message":"syntetisk-melding"},{"message":"syntetisk-null","locations":null,"path":null}]}
                """.trimIndent(),
                null,
            )

        assertThat(feil.errors)
            .containsExactly(
                PdlFeil(message = "syntetisk-melding"),
                PdlFeil(message = "syntetisk-null", locations = null, path = null),
            )
        feil.errors.forEach {
            assertThat(it.locations).isNull()
            assertThat(it.path).isNull()
        }
    }

    @Test
    fun `tomme PDL-meldinger feiler med standardtekst`() {
        val feil =
            assertFeil(
                """{"errors":[{"message":null},{"message":""},{"message":"   "}]}""",
                null,
            )
        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.feilmelding).isEqualTo("Svar fra PDL inneholder feil")
    }

    @Test
    fun `flere PDL-meldinger beholdes i samme feilmelding`() {
        val feil =
            assertFeil(
                """{"errors":[{"message":"Fant ikke person"},{"message":"Ikke tilgang"}]}""",
                null,
            )

        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.feilmelding).isEqualTo("Svar fra PDL inneholder feil: Fant ikke person; Ikke tilgang")
    }

    @Test
    fun `PDL-meldinger beholder kontrolltegn uten å eksponere responsdetaljer`() {
        val body =
            """
            {
              "data": "data-sentinel",
              "errors": [
                {
                  "message": "Fant\nikke\tperson\u2028klar",
                  "path": ["path-sentinel"],
                  "extensions": {
                    "code": "code-sentinel",
                    "details": {"type": "details-sentinel"}
                  }
                }
              ]
            }
            """.trimIndent()
        val feil = assertFeil(body, null)

        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.feilmelding).isEqualTo("Svar fra PDL inneholder feil: Fant\nikke\tperson\u2028klar")
        assertThat(feil.cause).isNull()
        assertThat(
            feil.errors
                .single()
                .extensions
                ?.details,
        ).isEqualTo(mapOf("type" to "details-sentinel"))
        assertThat(feil.toString()).doesNotContain("data-sentinel")
        assertThat(feil.message).doesNotContain(body)
        assertThat(
            feil.feilmelding,
        ).doesNotContain("path-sentinel", "code-sentinel", "details-sentinel", "data-sentinel")
        assertThat(feil.cause).isNull()
        assertThat(feil.suppressed).isEmpty()
    }

    @Test
    fun `mangler body eller toppdata gir en klientfeil`() {
        val tomtSvar = assertFeil("{}", null)
        assertThat(tomtSvar.message).isEqualTo("Svar fra PDL mangler data")
        assertThat(tomtSvar.cause).isNull()
        val nullData = assertFeil("""{"data":null}""", null)
        assertThat(nullData.message).isEqualTo("Svar fra PDL mangler data")
        assertThat(nullData.cause).isNull()

        val (restClient, server) = klientoppsett()
        server.expect(once(), requestTo("https://pdl.test/graphql")).andRespond(withStatus(HttpStatus.NO_CONTENT))
        val feil = hentFeil(PdlMaskinKlient(URI.create("https://pdl.test"), restClient))
        assertThat(feil).isInstanceOf(PdlException::class.java)
        assertThat(feil.message).isEqualTo("Svar fra PDL mangler body")
        assertThat((feil as PdlException).httpStatus).isNull()
        server.verify()
    }

    @Test
    fun `HTTP-feil beholder status og fast wrappermelding`() {
        listOf(400, 401, 403, 404, 429, 500).forEach { status ->
            val feil =
                assertHttpFeil(
                    """{"errors":[{"message":"Ikke tilgang","extensions":{"code":"unauthorized"}}]}""",
                    status,
                )
            assertThat(feil.message).isEqualTo("Kall mot PDL feilet med HTTP-status $status")
            assertThat(feil.errors).isEmpty()
            assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        }
    }

    @Test
    fun `HTTP-feil med HTML tom ugyldig eller IO-body bruker fast melding med cause`() {
        listOf(
            "<html>body-sentinel</html>" to "text/html",
            "" to MediaType.APPLICATION_JSON_VALUE,
            "{" to MediaType.APPLICATION_JSON_VALUE,
        ).forEach { (body, contentType) ->
            val feil = assertHttpFeil(body, 500, contentType, listOf(body))
            assertThat(feil.message).isEqualTo("Kall mot PDL feilet med HTTP-status 500")
            assertThat(feil.httpStatus).isEqualTo(500)
            assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        }

        val opprinneligIoFeil = IOException("io-body-sentinel")
        val respons = ResponsMedBodyfeil(HttpStatusCode.valueOf(403), opprinneligIoFeil)
        respons.headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        val ioFeil = assertHttpRespons(respons, 403, listOf("io-body-sentinel"))
        assertThat(ioFeil.httpStatus).isEqualTo(403)
        assertThat(ioFeil.message).isEqualTo("Kall mot PDL feilet med HTTP-status 403")
        assertThat(ioFeil.cause).isInstanceOf(RestClientResponseException::class.java)
        assertThat((ioFeil.cause as RestClientResponseException).statusCode.value()).isEqualTo(403)
    }

    @Test
    fun `ugyldig json og ugyldig errors-struktur gir lesefeil uten cause`() {
        listOf(
            "{",
            """{"data":"syntetisk-svar","errors":"ikke-en-liste"}""",
            """{"data":"syntetisk-svar","errors":[null]}""",
        ).forEach { body ->
            val feil = assertFeil(body, null, listOf(body, "syntetisk-svar", "ikke-en-liste"))
            assertThat(feil.message).startsWith("Svar fra PDL kunne ikke leses")
            assertThat(feil.httpStatus).isNull()
            assertThat(feil.cause).isNull()
        }
    }

    @Test
    fun `svar med ukjent innholdstype gir lesefeil uten cause`() {
        val (restClient, server) = klientoppsett()
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond(withSuccess("<html>fnr-sentinel</html>", MediaType.TEXT_HTML))

        val feil = hentFeil(PdlMaskinKlient(URI.create("https://pdl.test"), restClient))

        assertThat(feil.message).isEqualTo("Svar fra PDL kunne ikke leses")
        assertThat(feil.cause).isNull()
        assertThat(feil.stackTraceToString()).doesNotContain("fnr-sentinel")
        server.verify()
    }

    @Test
    fun `data som ikke passer typen gir lesefeil med sti uten verdier`() {
        val (restClient, server) = klientoppsett()
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond(
                withSuccess(
                    """{"data":{"personer":[{"dato":"fnr-sentinel"}]}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val feil =
            catchThrowable {
                PdlMaskinKlient(URI.create("https://pdl.test"), restClient).hent<TestPersoner>(GRAPHQL, emptyMap())
            } as PdlException

        assertThat(feil.message).isEqualTo("Svar fra PDL kunne ikke leses: [data.personer.[0].dato]")
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.cause).isNull()
        assertThat(feil.stackTraceToString()).doesNotContain("fnr-sentinel")
        server.verify()
    }

    @Test
    fun `graphql-feil bevares når dto følger nullbarheten i PDL-skjemaet`() {
        val (restClient, server) = klientoppsett()
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "data": {"hentPerson": null},
                      "errors": [{"message": "Fant ikke person", "extensions": {"code": "not_found"}}]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val feil =
            catchThrowable {
                PdlMaskinKlient(URI.create("https://pdl.test"), restClient).hent<TestSvarMedPerson>(GRAPHQL, emptyMap())
            } as PdlException

        assertThat(feil.message).isEqualTo("Svar fra PDL inneholder feil")
        assertThat(feil.errors).containsExactly(PdlFeil("Fant ikke person", PdlFeilExtensions("not_found")))
        assertThat(feil.cause).isNull()
        server.verify()
    }

    @Test
    fun `påkrevd felt som PDL sender som null gir lesefeil uten verdier`() {
        val (restClient, server) = klientoppsett()
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "data": {"hentPerson": null, "annet": "fnr-sentinel"},
                      "errors": [{"message": "Fant ikke person", "extensions": {"code": "not_found"}}]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val feil =
            catchThrowable {
                PdlMaskinKlient(URI.create("https://pdl.test"), restClient).hent<TestSvarMedPåkrevdPerson>(
                    GRAPHQL,
                    emptyMap(),
                )
            } as PdlException

        assertThat(feil.message).startsWith("Svar fra PDL kunne ikke leses").contains("hentPerson")
        assertThat(feil.errors).isEmpty()
        assertThat(feil.cause).isNull()
        assertThat(feil.stackTraceToString()).doesNotContain("fnr-sentinel")
        server.verify()
    }

    @Test
    fun `transportfeil beholder eksakt originalfeil som cause`() {
        val originalFeil = IllegalStateException("syntetisk-transportfeil")
        val restClient =
            RestClient
                .builder()
                .requestInterceptor { _, _, _ -> throw originalFeil }
                .build()

        val feil = hentFeil(PdlMaskinKlient(URI.create("https://pdl.test"), restClient))

        assertThat(feil.message).isEqualTo("Kall mot PDL feilet")
        assertThat(feil.cause).isSameAs(originalFeil)
    }

    @Test
    fun `exception har enkel melding og status uten cause`() {
        val standard = PdlException()
        val feil = PdlException("Linje\r\nmed\tkontroll\u0001\u0001skift\u2028klar", httpStatus = 403)
        val mutableErrors = mutableListOf(PdlFeil("syntetisk-melding"))
        val kopi = PdlException(errors = mutableErrors)
        mutableErrors.clear()

        assertThat(standard.message).isEqualTo("Kall mot PDL feilet")
        assertThat(standard.httpStatus).isNull()
        assertThat(feil.message).isEqualTo("Linje\r\nmed\tkontroll\u0001\u0001skift\u2028klar")
        assertThat(feil.httpStatus).isEqualTo(403)
        assertThat(feil.cause).isNull()
        assertThat(feil.suppressed).isEmpty()
        assertThat(kopi.errors).isSameAs(mutableErrors)
        assertThat(kopi.errors).isEmpty()

        val originalFeil = IllegalStateException("syntetisk-cause")
        val medCause = PdlException("syntetisk-melding", mutableErrors, 500, originalFeil)
        assertThat(medCause.cause).isSameAs(originalFeil)
    }

    private fun assertFeil(
        body: String,
        forventetStatus: Int?,
        skjulteVerdier: List<String> = emptyList(),
    ): PdlException {
        val (restClient, server) = klientoppsett()
        server
            .expect(once(), requestTo("https://pdl.test/graphql"))
            .andRespond { _ -> mockRespons(body, HttpStatus.OK, MediaType.APPLICATION_JSON_VALUE) }
        val feil = hentFeil(PdlMaskinKlient(URI.create("https://pdl.test"), restClient))
        assertThat(feil.httpStatus).isEqualTo(forventetStatus)
        assertThat(feil.suppressed).isEmpty()
        skjulteVerdier.filter(String::isNotEmpty).forEach { assertThat(feil.stackTraceToString()).doesNotContain(it) }
        server.verify()
        return feil
    }

    private fun assertHttpFeil(
        body: String,
        forventetStatus: Int,
        contentType: String? = MediaType.APPLICATION_JSON_VALUE,
        skjulteVerdier: List<String> = emptyList(),
    ): PdlException =
        assertHttpRespons(
            mockRespons(body, HttpStatusCode.valueOf(forventetStatus), contentType),
            forventetStatus,
            skjulteVerdier,
        )

    private fun assertHttpRespons(
        respons: MockClientHttpResponse,
        forventetStatus: Int,
        skjulteVerdier: List<String> = emptyList(),
    ): PdlException {
        val (restClient, server) = klientoppsett()
        server.expect(once(), requestTo("https://pdl.test/graphql")).andRespond { respons }
        val feil = hentFeil(PdlMaskinKlient(URI.create("https://pdl.test"), restClient))
        assertThat(feil.httpStatus).isEqualTo(forventetStatus)
        skjulteVerdier.filter(String::isNotEmpty).forEach { assertThat(feil.toString()).doesNotContain(it) }
        server.verify()
        return feil
    }

    private fun hentFeil(klient: PdlKlient): PdlException {
        val feil = catchThrowable { klient.hent<String>(GRAPHQL, mapOf("ident" to "ident-sentinel")) }
        assertThat(feil).isInstanceOf(PdlException::class.java)
        return feil as PdlException
    }

    private fun mockRespons(
        body: String,
        status: HttpStatusCode,
        contentType: String?,
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
    )

    private data class TestSvar(
        val verdi: String,
        val person: TestPerson?,
    )

    private data class TestSvarMedPerson(
        val hentPerson: TestPerson?,
    )

    private data class TestSvarMedPåkrevdPerson(
        val hentPerson: TestPerson,
    )

    private data class TestDato(
        val dato: LocalDate,
    )

    private data class TestPersoner(
        val personer: List<TestDato>,
    )

    private fun klientoppsett(): Pair<RestClient, MockRestServiceServer> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        return builder.build() to server
    }

    private companion object {
        const val GRAPHQL = "query SyntetiskOperasjon { ping }"
    }
}
