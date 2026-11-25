package no.nav.familie.pdl

import com.sun.net.httpserver.HttpServer
import no.nav.familie.felles.klient.retry.KlientRetryProperties
import no.nav.familie.felles.tokenklient.entraid.EntraIDClient
import no.nav.familie.felles.tokenklient.entraid.MaskinTilMaskinTokenInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.test.web.client.ExpectedCount.twice
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.io.InputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URI.create
import java.time.Duration.ofMillis
import java.util.concurrent.atomic.AtomicInteger

class PdlRetryIntegrasjonTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val klient =
        PdlMaskinKlient(
            URI.create(BASE_URL),
            builder.build(),
            "B123",
            KlientRetryProperties(true, 1, ofMillis(1)),
        )

    @AfterEach
    fun verifiserKall() {
        server.verify()
    }

    @ParameterizedTest
    @ValueSource(ints = [502, 503, 504])
    fun `midlertidige HTTP-feil fra PDL gir nytt forsøk`(status: Int) {
        // Arrange
        server
            .expect(requestTo(URL))
            .andExpect(header("Behandlingsnummer", "B999"))
            .andRespond { svar(status) }

        server
            .expect(requestTo(URL))
            .andExpect(header("Behandlingsnummer", "B999"))
            .andRespond { svar(200) }

        // Act
        val svar = klient.hent<String>(QUERY, emptyMap(), behandlingsnummer = "B999")

        // Assert
        assertThat(svar).isEqualTo("svar")
    }

    @ParameterizedTest
    @ValueSource(ints = [500, 501, 505])
    fun `andre HTTP-feil gir ett forsøk`(status: Int) {
        // Arrange
        server
            .expect(requestTo(URL))
            .andRespond { svar(status) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(QUERY, emptyMap()) }

        // Assert
        assertThat(feil.httpStatus).isEqualTo(status)
        assertThat(feil.message).isEqualTo("Kall mot PDL feilet med HTTP-status $status")
        assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        assertThat(feil.suppressed).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("transportfeil")
    fun `transportfeil mot PDL gir ett forsøk`(transportfeil: Exception) {
        // Arrange
        server
            .expect(requestTo(URL))
            .andRespond { throw transportfeil }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(QUERY, emptyMap()) }

        // Assert
        assertThat(feil.cause).isInstanceOf(ResourceAccessException::class.java)
        assertThat(feil.cause?.cause).isSameAs(transportfeil)
    }

    @Test
    fun `brutt body-lesing etter HTTP 200 gir ikke retry`() {
        // Arrange
        server
            .expect(requestTo(URL))
            .andRespond {
                object : MockClientHttpResponse(ByteArray(0), OK) {
                    override fun getBody(): InputStream =
                        object : InputStream() {
                            override fun read(): Int = throw SocketTimeoutException("Body kunne ikke leses")
                        }
                }
            }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(QUERY, emptyMap()) }

        // Assert
        assertThat(feil.httpStatus).isNull()
    }

    @Test
    fun `503 fra tokenhenting i en interceptor gir nytt forsøk`() {
        // Arrange
        val antallTokenforsøk = AtomicInteger()
        val klient =
            PdlMaskinKlient(
                create(BASE_URL),
                builder
                    .requestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
                        if (antallTokenforsøk.incrementAndGet() == 1) throw HttpServerErrorException(SERVICE_UNAVAILABLE)
                        execution.execute(request, body)
                    }.build(),
                "B123",
                KlientRetryProperties(true, 1, ofMillis(1)),
            )

        server
            .expect(requestTo(URL))
            .andRespond { svar(200) }

        // Act
        val svar = klient.hent<String>(QUERY, emptyMap())

        // Assert
        assertThat(svar).isEqualTo("svar")
        assertThat(antallTokenforsøk.get()).isEqualTo(2)
    }

    @ParameterizedTest
    @MethodSource("andreTokenfeil")
    fun `andre feil fra tokenhenting gir ikke nytt forsøk`(opprinneligFeil: Exception) {
        // Arrange
        val antallTokenforsøk = AtomicInteger()
        val klient =
            PdlMaskinKlient(
                create(BASE_URL),
                builder
                    .requestInterceptor { _: HttpRequest, _: ByteArray, _: ClientHttpRequestExecution ->
                        antallTokenforsøk.incrementAndGet()
                        throw opprinneligFeil
                    }.build(),
                "B123",
                KlientRetryProperties(true, 1, ofMillis(1)),
            )

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(QUERY, emptyMap()) }

        // Assert
        assertThat(antallTokenforsøk.get()).isEqualTo(1)
        assertThat(feil.cause).isSameAs(opprinneligFeil)
    }

    @ParameterizedTest
    @ValueSource(longs = [1, 2])
    fun `TokenHenter gjentar tokenkall for hvert PDL-forsøk ved vedvarende 503`(maxRetries: Long) {
        // Arrange
        val antallTokenkall = AtomicInteger()
        val tokenserver =
            HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
                createContext("/token") { exchange ->
                    exchange.use {
                        antallTokenkall.incrementAndGet()
                        it.requestBody.readAllBytes()
                        val body = "token".toByteArray()
                        it.sendResponseHeaders(503, body.size.toLong())
                        it.responseBody.write(body)
                    }
                }
                start()
            }

        try {
            val tokenEndpoint = "http://127.0.0.1:${tokenserver.address.port}/token"
            val entraIDClient = EntraIDClient(tokenEndpoint, tokenEndpoint)
            val tokenBuilder =
                RestClient
                    .builder()
                    .requestInterceptor(MaskinTilMaskinTokenInterceptor(entraIDClient, "api://pdl.test.app/.default"))
            val pdlServer = MockRestServiceServer.bindTo(tokenBuilder).build()
            val klient =
                PdlMaskinKlient(
                    url = URI.create(BASE_URL),
                    restClient = tokenBuilder.build(),
                    behandlingsnummer = "B123",
                    retry = KlientRetryProperties(maxRetries = maxRetries, delay = ofMillis(1)),
                )

            // Act
            val feil = assertThrows<PdlException> { klient.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(antallTokenkall.get()).isEqualTo((2 * (maxRetries + 1)).toInt())
            assertThat(feil.httpStatus).isEqualTo(503)
            assertThat(feil.cause).isInstanceOf(HttpServerErrorException.ServiceUnavailable::class.java)
            assertThat(feil.suppressed).isEmpty()
            pdlServer.verify()
        } finally {
            tokenserver.stop(0)
        }
    }

    @Test
    fun `avslått retry gjør ett forsøk og beholder siste feil`() {
        // Arrange
        val klient =
            PdlMaskinKlient(
                URI.create(BASE_URL),
                builder.build(),
                "B123",
                KlientRetryProperties(enabled = false, maxRetries = 1, delay = ofMillis(1)),
            )
        server
            .expect(requestTo(URL))
            .andRespond { svar(503) }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(QUERY, emptyMap()) }

        // Assert
        assertThat(feil.httpStatus).isEqualTo(503)
        assertThat(feil.suppressed).isEmpty()
    }

    @Test
    fun `siste feil returneres uendret uten tidligere forsøk vedlagt`() {
        // Arrange
        server
            .expect(twice(), requestTo(URL))
            .andRespond { svar(503, "responsbody") }

        // Act
        val feil = assertThrows<PdlException> { klient.hent<String>(QUERY, emptyMap()) }

        // Assert
        val sisteFeil = feil.cause as RestClientResponseException
        assertThat(sisteFeil.responseBodyAsString).contains("responsbody")
        assertThat(feil.suppressed).isEmpty()
        assertThat(sisteFeil.suppressed).isEmpty()
    }

    private fun svar(
        status: Int,
        body: String = """{"data":"svar"}""",
    ): MockClientHttpResponse =
        MockClientHttpResponse(body.toByteArray(), HttpStatusCode.valueOf(status)).apply {
            headers.contentType = MediaType.APPLICATION_JSON
        }

    companion object {
        private const val BASE_URL = "https://pdl.test"
        private const val URL = "$BASE_URL/graphql"
        private const val QUERY = "query { ping }"

        @JvmStatic
        fun transportfeil() =
            listOf(
                ConnectException("Connection refused"),
                SocketTimeoutException("Read timed out"),
            )

        @JvmStatic
        fun andreTokenfeil() =
            listOf(
                HttpServerErrorException(INTERNAL_SERVER_ERROR),
                ResourceAccessException("Tokenkall feilet", SocketTimeoutException()),
                IllegalStateException("Mangler brukertoken"),
            )
    }
}
