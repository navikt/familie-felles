package no.nav.familie.oppgave

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import no.nav.familie.oppgave.modell.PatchOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.UnknownContentTypeException
import java.net.SocketTimeoutException
import java.net.URI
import java.time.LocalDate

class OppgaveFeilhåndteringTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var klient: OppgaveMaskinKlient
    private var originalCallId: String? = null

    @BeforeEach
    fun setUp() {
        originalCallId = MDC.get(MDCConstants.MDC_CALL_ID)
        MDC.remove(MDCConstants.MDC_CALL_ID)
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        klient = OppgaveMaskinKlient(URI.create(BASE_URL), builder.build())
    }

    @AfterEach
    fun verifyServer() {
        try {
            server.verify()
        } finally {
            if (originalCallId == null) {
                MDC.remove(MDCConstants.MDC_CALL_ID)
            } else {
                MDC.put(MDCConstants.MDC_CALL_ID, originalCallId)
            }
        }
    }

    @Test
    fun `HTTP-feil med gyldig feilmelding setter message til feilmeldingen og beholder metadata og original respons`() {
        // Arrange
        val feilmelding = "  første linje\r\nandre linje  "
        val mdcCallId = "mdc-call-id"
        MDC.put(MDCConstants.MDC_CALL_ID, mdcCallId)
        val body =
            """{"feilmelding":"  første linje\r\nandre linje  ","uuid":"$CORRELATION_ID","ukjent":"syntetisk"}"""
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/1"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(CORRELATION_ID_HEADER, mdcCallId))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body))

        // Act
        val feil = assertThrows<OppgaveException> { klient.hentOppgave(1) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.HTTP)
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(feil.operasjon).isEqualTo(OppgaveOperasjon.HENT_OPPGAVE)
        assertThat(feil.korrelasjonsId).isEqualTo(mdcCallId)
        assertThat(feil.message).isEqualTo(feilmelding)
        assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        val original = feil.cause as RestClientResponseException
        assertThat(original.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(original.responseBodyAsString).isEqualTo(body)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "{\"feilmelding\":\" \"}",
            "{\"feilmelding\":123}",
            "{\"feilmelding\":\"avbrutt\"",
            "{}",
        ],
    )
    fun `HTTP-feil med ugyldig feilmelding gir default message men beholder øvrig metadata og original respons`(body: String) {
        // Arrange
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body))

        // Act
        val feil = assertThrows<OppgaveException> { klient.hentOppgave(1, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.HTTP)
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(feil.operasjon).isEqualTo(OppgaveOperasjon.HENT_OPPGAVE)
        assertThat(feil.korrelasjonsId).isEqualTo(CORRELATION_ID)
        assertThat(feil).hasMessageStartingWith("Oppgave-kall feilet")
        assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        val original = feil.cause as RestClientResponseException
        assertThat(original.responseBodyAsString).isEqualTo(body)
    }

    @Test
    fun `HTTP 200 med tom body gir ugyldig respons uten cause`() {
        // Arrange
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        // Act
        val feil = assertThrows<OppgaveException> { klient.hentOppgave(1, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.UGYLDIG_RESPONS)
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.cause).isNull()
    }

    @Test
    fun `ugyldig JSON gir ugyldig respons med parse-cause`() {
        // Arrange
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{", MediaType.APPLICATION_JSON))

        // Act
        val feil = assertThrows<OppgaveException> { klient.hentOppgave(1, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.UGYLDIG_RESPONS)
        assertThat(feil.cause).isNotNull()
    }

    @Test
    fun `manglende påkrevd felt gir ugyldig respons med parse-cause`() {
        // Arrange
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"id":1,"tema":"BID","oppgavetype":"TEST","versjon":1,"prioritet":"NORM","status":"OPPRETTET","aktivDato":"2026-01-01"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        // Act
        val feil = assertThrows<OppgaveException> { klient.hentOppgave(1, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.UGYLDIG_RESPONS)
        assertThat(feil.cause).isNotNull()
    }

    @Test
    fun `ukjent content type gir ugyldig respons med original cause`() {
        // Arrange
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("opaque", MediaType.TEXT_PLAIN))

        // Act
        val feil = assertThrows<OppgaveException> { klient.hentOppgave(1, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.UGYLDIG_RESPONS)
        assertThat(feil.cause).isInstanceOf(UnknownContentTypeException::class.java)
    }

    @Test
    fun `POST HTTP 503 gir HTTP-feil`() {
        // Arrange
        val body = """{"feilmelding":"syntetisk utilgjengelig"}"""
        val request = OpprettOppgaveRequest(LocalDate.parse("2026-01-01"), "TEST", "NORM", "BID")
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON).body(body))

        // Act
        val feil = assertThrows<OppgaveException> { klient.opprettOppgave(request, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.HTTP)
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value())
        assertThat(feil.operasjon).isEqualTo(OppgaveOperasjon.OPPRETT_OPPGAVE)
        assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
        assertThat((feil.cause as RestClientResponseException).responseBodyAsString).isEqualTo(body)
    }

    @Test
    fun `PATCH timeout gir transportfeil`() {
        // Arrange
        val timeout = SocketTimeoutException("syntetisk timeout")
        val request = PatchOppgaveRequest(versjon = 1)
        server
            .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/1"))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withException(timeout))

        // Act
        val feil = assertThrows<OppgaveException> { klient.patchOppgave(1, request, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.TRANSPORT)
        assertThat(feil.httpStatus).isNull()
        assertThat(feil.operasjon).isEqualTo(OppgaveOperasjon.PATCH_OPPGAVE)
        assertThat(feil.cause).isInstanceOf(ResourceAccessException::class.java)
        assertThat(feil.cause?.cause).isSameAs(timeout)
    }

    @Test
    fun `exception som kastes i request-interceptor gir OppgaveException og transportfeil med original exception i cause`() {
        // Arrange
        val original = IllegalStateException("syntetisk feil")
        val klient = klientSomKasterFørUtgåendeKall(original)

        // Act
        val feil = assertThrows<OppgaveException> { klient.hentOppgave(1, CORRELATION_ID) }

        // Assert
        assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.TRANSPORT)
        assertThat(feil.cause).isSameAs(original)
    }

    @Test
    fun `Error skal ikke føre til OppgaveException men kastes videre`() {
        // Arrange
        val original = AssertionError("syntetisk feil")
        val klient = klientSomKasterFørUtgåendeKall(original)

        // Act
        val feil = assertThrows<AssertionError> { klient.hentOppgave(1, CORRELATION_ID) }

        // Assert
        assertThat(feil).isSameAs(original)
    }

    @Test
    fun `direkte InterruptedException gir transportfeil og setter isInterrupted flagg til true`() {
        // Arrange
        val original = InterruptedException("syntetisk avbrudd")
        val klient = klientSomKasterFørUtgåendeKall(original)

        try {
            // Act
            val feil = assertThrows<OppgaveException> { klient.hentOppgave(1, CORRELATION_ID) }

            // Assert
            assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.TRANSPORT)
            assertThat(feil.cause).isSameAs(original)
            assertThat(Thread.currentThread().isInterrupted).isTrue()
        } finally {
            Thread.interrupted()
        }
    }

    private fun klientSomKasterFørUtgåendeKall(feil: Throwable): OppgaveMaskinKlient {
        val restClient =
            RestClient
                .builder()
                .requestInterceptor { _, _, _ -> throw feil }
                .build()
        return OppgaveMaskinKlient(URI.create(BASE_URL), restClient)
    }

    private companion object {
        const val BASE_URL = "https://oppgave.test"
        const val CORRELATION_ID = "f6d8b63d-552c-4bf8-a7bd-22f0b4d62127"
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
    }
}
