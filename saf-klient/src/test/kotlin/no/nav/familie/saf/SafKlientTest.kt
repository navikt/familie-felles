package no.nav.familie.saf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.MediaType.ALL_VALUE
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI

class SafKlientTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val klient = SafOboKlient(URI.create(BASE_URL), builder.build())

    @AfterEach
    fun verifiserKall() {
        server.verify()
    }

    @Nested
    inner class Hent {
        @Test
        fun `sender graphql-post med uendret query og variables`() {
            // Arrange
            val query =
                $$"""
                query Test($id: String!) {
                  verdi(id: $id)
                }
                """.trimIndent()
            val variabler = mapOf("id" to "id")
            val forventetJson =
                $$"""
                {
                  "query": "query Test($id: String!) {\n  verdi(id: $id)\n}",
                  "variables": {"id":"id"}
                }
                """.trimIndent()

            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(forventetJson, JsonCompareMode.STRICT))
                .andRespond { mockRespons("""{"data":{"verdi":"verdi"}}""") }

            // Act
            val svar = klient.hent<Map<String, String>>(query, variabler)

            // Assert
            assertThat(svar).containsEntry("verdi", "verdi")
        }

        @Test
        fun `bevarer nøstede generiske typer`() {
            // Arrange
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons("""{"data":{"gruppe":[{"navn":"navn"}]}}""") }

            // Act
            val svar = klient.hent<Map<String, List<Map<String, String>>>>(QUERY, emptyMap())

            // Assert
            assertThat(svar.getValue("gruppe")).containsExactly(mapOf("navn" to "navn"))
        }

        @Test
        fun `tillater null felt i data`() {
            // Arrange
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons("""{"data":{"verdi":null}}""") }

            // Act
            val svar = klient.hent<Map<String, String?>>(QUERY, emptyMap())

            // Assert
            assertThat(svar).containsKey("verdi")
            assertThat(svar.getValue("verdi")).isNull()
        }

        @Test
        fun `graphql errors med partial data bevarer feilene fra Saf`() {
            // Arrange
            val code = "code"
            val message = "message"
            val path = "path"
            val classification = "classification"
            val resultat = "resultat"
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond {
                    mockRespons(
                        """{"data":{"resultat":"$resultat"},"errors":[{"message":"$message","locations":[{"line":1,"column":2}],"path":["$path",0],"extensions":{"code":"$code","classification":"$classification"}}]}""",
                    )
                }

            // Act
            val feil = assertThrows<SafException> { klient.hent<Map<String, String>>(QUERY, emptyMap()) }

            // Assert
            val safFeil = feil.errors.single()
            assertThat(safFeil.message).isEqualTo(message)
            assertThat(safFeil.locations).containsExactly(mapOf("line" to 1, "column" to 2))
            assertThat(safFeil.path).containsExactly(path, 0)
            assertThat(safFeil.extensions?.code).isEqualTo(code)
            assertThat(safFeil.extensions?.classification).isEqualTo(classification)
            assertThat(feil.message).isEqualTo("Svar fra Saf inneholder feil")
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
        }

        @Test
        fun `graphql errors uten extensions eller code bevarer null code`() {
            // Arrange
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons("""{"errors":[{"message":"message"},{"extensions":{}}]}""") }

            // Act
            val feil = assertThrows<SafException> { klient.hent<Map<String, String>>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.errors.map { it.extensions?.code }).containsExactly(null, null)
            assertThat(feil.httpStatus).isNull()
        }

        @ParameterizedTest
        @CsvSource(
            "forbidden, 403",
            "not_found, 404",
            "bad_request, 400",
            "server_error, 500",
            "ukjent,",
        )
        fun `graphql-kode gir utledet http-status`(
            code: String,
            forventetStatus: Int?,
        ) {
            // Arrange
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons("""{"errors":[{"extensions":{"code":"$code"}}]}""") }

            // Act
            val feil = assertThrows<SafException> { klient.hent<Map<String, String>>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.httpStatus).isEqualTo(forventetStatus)
            assertThat(feil.cause).isNull()
        }

        @ParameterizedTest
        @CsvSource(
            delimiter = '|',
            value = [
                "server_error,bad_request,not_found,forbidden | 403",
                "server_error,bad_request,not_found | 404",
                "server_error,bad_request | 400",
                "ukjent,server_error | 500",
            ],
        )
        fun `flere graphql-koder gir status etter prioritet`(
            koder: String,
            forventetStatus: Int,
        ) {
            // Arrange
            val errors = koder.split(",").joinToString(",") { """{"extensions":{"code":"$it"}}""" }
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons("""{"errors":[$errors]}""") }

            // Act
            val feil = assertThrows<SafException> { klient.hent<Map<String, String>>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.httpStatus).isEqualTo(forventetStatus)
        }

        @ParameterizedTest
        @ValueSource(ints = [403, 404])
        fun `http-feil bevarer status og rå body i cause uten å eksponere den i meldingen`(status: Int) {
            // Arrange
            val body = "body"
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons(body, HttpStatusCode.valueOf(status)) }

            // Act
            val feil = assertThrows<SafException> { klient.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.httpStatus).isEqualTo(status)
            assertThat(feil.message).isEqualTo("Kall mot Saf feilet med HTTP-status $status")
            assertThat(feil.message).doesNotContain(body)
            assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
            assertThat((feil.cause as RestClientResponseException).responseBodyAsString).isEqualTo(body)
            assertThat(feil.suppressed).isEmpty()
        }

        @ParameterizedTest
        @CsvSource(
            delimiter = '|',
            value = ["''|200", "{|200", "{}|200", """{"data":null}|200"""],
        )
        fun `manglende eller ugyldig graphql-body gir trygg klientfeil`(
            body: String,
            status: Int,
        ) {
            // Arrange
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons(body, HttpStatusCode.valueOf(status)) }

            // Act
            val feil = assertThrows<SafException> { klient.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.httpStatus).isNull()
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
            if (body.isNotEmpty()) {
                assertThat(feil.message).doesNotContain(body)
                assertThat(feil.stackTraceToString()).doesNotContain(body)
            }
        }

        @Test
        fun `transportfeil bevarer original cause uten suppressed`() {
            // Arrange
            val message = "message"
            val originalFeil = IllegalStateException(message)
            val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
            val klientMedTransportfeil = SafOboKlient(URI.create(BASE_URL), restClient)

            // Act
            val feil = assertThrows<SafException> { klientMedTransportfeil.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.message).isEqualTo("Kall mot Saf feilet")
            assertThat(feil.message).doesNotContain(message)
            assertThat(feil.cause).isSameAs(originalFeil)
            assertThat(feil.suppressed).isEmpty()
        }

        @Test
        fun `ukjent innholdstype med bytes saneres uten cause`() {
            // Arrange
            val body = "body"
            val bytes = body.toByteArray()
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond {
                    MockClientHttpResponse(bytes, HttpStatus.OK).apply {
                        headers.contentType = MediaType("application", "x-saf-unknown")
                    }
                }

            // Act
            val feil = assertThrows<SafException> { klient.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.message).isEqualTo("Svar fra Saf kunne ikke leses")
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
            assertThat(feil.stackTraceToString()).doesNotContain(body)
        }

        @Test
        fun `konverteringsfeil saneres uten cause`() {
            // Arrange
            val message = "message"
            val originalFeil = HttpMessageConversionException(message)
            val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
            val klientMedLesefeil = SafOboKlient(URI.create(BASE_URL), restClient)

            // Act
            val feil = assertThrows<SafException> { klientMedLesefeil.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.message).isEqualTo("Svar fra Saf kunne ikke leses")
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
            assertThat(feil.stackTraceToString()).doesNotContain(message)
        }

        @Test
        fun `lesefeil dypt i årsakskjeden saneres`() {
            // Arrange
            val message = "message"
            val lesefeil = HttpMessageNotReadableException(message, MockHttpInputMessage(byteArrayOf()))
            val originalFeil = RestClientException(message, IllegalStateException(message, lesefeil))
            val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
            val klientMedLesefeil = SafOboKlient(URI.create(BASE_URL), restClient)

            // Act
            val feil = assertThrows<SafException> { klientMedLesefeil.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.message).isEqualTo("Svar fra Saf kunne ikke leses")
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
            assertThat(feil.stackTraceToString()).doesNotContain(message)
        }

        @Test
        fun `jackson-feil i årsakskjeden gir feltsti`() {
            // Arrange
            val verdi = "verdi"
            val jacksonFeil =
                assertThrows<JacksonException> {
                    jacksonObjectMapper().readValue<TestJacksonType>("""{"tall":"$verdi"}""")
                }
            val originalFeil = RestClientException("message", IllegalStateException("message", jacksonFeil))
            val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
            val klientMedLesefeil = SafOboKlient(URI.create(BASE_URL), restClient)

            // Act
            val feil = assertThrows<SafException> { klientMedLesefeil.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.message).isEqualTo("Svar fra Saf kunne ikke leses: [tall]")
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
            assertThat(feil.stackTraceToString()).doesNotContain(verdi)
        }

        @ParameterizedTest
        @ValueSource(booleans = [false, true])
        fun `lesefeil i objekt eller array inkluderer Jackson-sti`(erArray: Boolean) {
            // Arrange
            val verdi = "verdi"
            val body =
                if (erArray) {
                    """{"data":{"liste":[{"tall":"$verdi"}]}}"""
                } else {
                    """{"data":{"objekt":{"tall":"$verdi"}}}"""
                }
            server
                .expect(once(), requestTo(GRAPHQL_URL))
                .andRespond { mockRespons(body) }

            // Act
            val feil =
                assertThrows<SafException> {
                    if (erArray) {
                        klient.hent<Map<String, List<TestJacksonType>>>(QUERY, emptyMap())
                    } else {
                        klient.hent<Map<String, TestJacksonType>>(QUERY, emptyMap())
                    }
                }

            // Assert
            val forventetSti = if (erArray) "data.liste.[0].tall" else "data.objekt.tall"
            val forventetMelding = "Svar fra Saf kunne ikke leses: [$forventetSti]"
            assertThat(feil.message).isEqualTo(forventetMelding)
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
            assertThat(feil.stackTraceToString()).doesNotContain(verdi)
        }

        @ParameterizedTest
        @ValueSource(booleans = [false, true])
        fun `Error kastes identisk med og uten lesefeilårsak`(medLesefeilÅrsak: Boolean) {
            // Arrange
            val originalFeil = AssertionError("message")
            if (medLesefeilÅrsak) {
                originalFeil.initCause(HttpMessageConversionException("message"))
            }
            val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
            val klientMedError = SafOboKlient(URI.create(BASE_URL), restClient)

            // Act
            val feil = assertThrows<AssertionError> { klientMedError.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil).isSameAs(originalFeil)
        }
    }

    @Nested
    inner class HentDokument {
        @Test
        fun `henter dokument med riktig url og aksepterer bytes som ikke er pdf`() {
            // Arrange
            val bytes = byteArrayOf(0, 1, 2, 0x7f, 0xff.toByte())
            server
                .expect(once(), requestTo(DOKUMENT_URL))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, ALL_VALUE))
                .andRespond {
                    MockClientHttpResponse(bytes, HttpStatus.OK).apply {
                        headers.contentType = MediaType.TEXT_PLAIN
                    }
                }

            // Act
            val dokument = klient.hentDokument(JOURNALPOST_ID, DOKUMENT_INFO_ID)

            // Assert
            assertThat(dokument).containsExactly(*bytes)
        }

        @Test
        fun `enkoder journalpost-id som path-segment`() {
            // Arrange
            val bytes = byteArrayOf(1, 2)
            server
                .expect(once(), requestTo("$BASE_URL/rest/hentdokument/a%2Fb%20c/$DOKUMENT_INFO_ID/ARKIV"))
                .andRespond { MockClientHttpResponse(bytes, HttpStatus.OK) }

            // Act
            val dokument = klient.hentDokument("a/b c", DOKUMENT_INFO_ID)

            // Assert
            assertThat(dokument).containsExactly(*bytes)
        }

        @Test
        fun `tom dokument-body gir klientfeil`() {
            // Arrange
            server
                .expect(once(), requestTo(DOKUMENT_URL))
                .andRespond { MockClientHttpResponse(byteArrayOf(), HttpStatus.OK) }

            // Act
            val feil = assertThrows<SafException> { klient.hentDokument(JOURNALPOST_ID, DOKUMENT_INFO_ID) }

            // Assert
            assertThat(feil).hasMessage("Svar fra Saf mangler body")
        }

        @Test
        fun `dokument-get bevarer http cause fra interceptor`() {
            // Arrange
            val originalFeil = HttpServerErrorException(HttpStatus.BAD_GATEWAY, "message")
            val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
            val klientMedHttpfeil = SafOboKlient(URI.create(BASE_URL), restClient)

            // Act
            val feil = assertThrows<SafException> { klientMedHttpfeil.hentDokument(JOURNALPOST_ID, DOKUMENT_INFO_ID) }

            // Assert
            assertThat(feil.httpStatus).isEqualTo(502)
            assertThat(feil.cause).isSameAs(originalFeil)
            assertThat(feil.message).doesNotContain("message")
            assertThat(feil.suppressed).isEmpty()
        }

        @Test
        fun `dokument-get sanerer lesefeil fra interceptor`() {
            // Arrange
            val message = "message"
            val originalFeil = HttpMessageConversionException(message)
            val restClient = RestClient.builder().requestInterceptor { _, _, _ -> throw originalFeil }.build()
            val klientMedLesefeil = SafOboKlient(URI.create(BASE_URL), restClient)

            // Act
            val feil = assertThrows<SafException> { klientMedLesefeil.hentDokument(JOURNALPOST_ID, DOKUMENT_INFO_ID) }

            // Assert
            assertThat(feil.message).isEqualTo("Svar fra Saf kunne ikke leses")
            assertThat(feil.cause).isNull()
            assertThat(feil.suppressed).isEmpty()
            assertThat(feil.stackTraceToString()).doesNotContain(message)
        }
    }

    private fun mockRespons(
        body: String,
        status: HttpStatusCode = HttpStatus.OK,
    ) = MockClientHttpResponse(body.toByteArray(), status).apply {
        headers.contentType = MediaType.APPLICATION_JSON
    }

    companion object {
        private const val BASE_URL = "https://saf.test"
        private const val GRAPHQL_URL = "$BASE_URL/graphql"
        private const val JOURNALPOST_ID = "12345678901"
        private const val DOKUMENT_INFO_ID = "98765432109"
        private const val DOKUMENT_URL = "$BASE_URL/rest/hentdokument/$JOURNALPOST_ID/$DOKUMENT_INFO_ID/ARKIV"
        private const val QUERY = "query { ping }"
    }

    private data class TestJacksonType(
        val tall: Int,
    )
}
