package no.nav.familie.oppgave

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.oppgave.modell.FinnOppgaverRequest
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import no.nav.familie.oppgave.modell.PatchFelt
import no.nav.familie.oppgave.modell.PatchOppgaveRequest
import no.nav.familie.oppgave.modell.RegistrerKommentarJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OppgaveKontraktTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val klient: OppgaveKlient = OppgaveMaskinKlient(URI.create("$BASE_URL/"), builder.build())
    private var originalCallId: String? = null

    @BeforeEach
    fun lagreOgFjerneMdcCallId() {
        originalCallId = MDC.get(MDCConstants.MDC_CALL_ID)
        MDC.remove(MDCConstants.MDC_CALL_ID)
    }

    @AfterEach
    fun verifiserKall() {
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

    @Nested
    inner class HentOppgave {
        @Test
        fun `skal sette forventede headere på request og kunne parse gyldig respons`() {
            // Arrange
            val korrelasjonsId = "eksplisitt-call-id"
            MDC.put(MDCConstants.MDC_CALL_ID, "annen-call-id")
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(CORRELATION_ID_HEADER, korrelasjonsId))
                .andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))

            // Act
            val oppgave = klient.hentOppgave(321, korrelasjonsId)

            // Assert
            assertThat(oppgave.id).isEqualTo(321)
            assertThat(oppgave.status).isEqualTo("NY_FREMTIDIG_STATUS")
            assertThat(oppgave.prioritet).isEqualTo("NY_FREMTIDIG_PRIORITET")
            assertThat(oppgave.bruker?.ident).isEqualTo("syntetisk-ident")
            assertThat(oppgave.bruker?.type).isEqualTo("NY_FREMTIDIG_BRUKERTYPE")
        }

        @Test
        fun `genererer korrelasjonsid med versjon fire når ingen er oppgitt`() {
            // Arrange
            MDC.remove(MDCConstants.MDC_CALL_ID)
            lateinit var generertKorrelasjonsId: UUID
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect {
                    generertKorrelasjonsId = UUID.fromString(checkNotNull(it.headers.getFirst(CORRELATION_ID_HEADER)))
                }.andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))

            // Act
            val oppgave = klient.hentOppgave(321)

            // Assert
            assertThat(oppgave.id).isEqualTo(321)
            assertThat(generertKorrelasjonsId.version()).isEqualTo(4)
        }

        @Test
        fun `leser callId fra MDC på nytt for hvert kall`() {
            // Arrange
            val førsteCallId = "forste-call-id"
            val andreCallId = "andre-call-id"
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(CORRELATION_ID_HEADER, førsteCallId))
                .andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(CORRELATION_ID_HEADER, andreCallId))
                .andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))

            // Act
            MDC.put(MDCConstants.MDC_CALL_ID, førsteCallId)
            val førsteOppgave = klient.hentOppgave(321)
            MDC.put(MDCConstants.MDC_CALL_ID, andreCallId)
            val andreOppgave = klient.hentOppgave(321)

            // Assert
            assertThat(førsteOppgave.id).isEqualTo(321)
            assertThat(andreOppgave.id).isEqualTo(321)
        }
    }

    @Nested
    inner class FinnOppgaver {
        @Test
        fun `skal sette alle innsendte felter i query-string parse gyldig respons`() {
            // Arrange
            val korrelasjonsId = "f6d8b63d-552c-4bf8-a7bd-22f0b4d62127"
            val request =
                FinnOppgaverRequest(
                    statuskategori = "AAPEN",
                    tema = listOf("BAR", "KON"),
                    oppgavetype = listOf("JFR", "BEH_SAK"),
                    aktoerId = listOf("aktor-a", "aktor-b"),
                    journalpostId = listOf("journal-a", "journal-b"),
                    saksreferanse = listOf("sak+verdi&med#tegn{par}"),
                    orgnr = listOf("orgnr-test"),
                    tildeltEnhetsnr = "enhet-test",
                    tilordnetRessurs = "ressurs-test",
                    behandlingstema = "tema-test",
                    behandlingstype = "type-test",
                    ferdigstiltFom = LocalDateTime.of(2026, 4, 3, 5, 6, 7),
                    ferdigstiltTom = LocalDateTime.of(2026, 4, 8, 9, 10, 11),
                    limit = 1000,
                    offset = 17,
                )
            val expectedUrl =
                "$BASE_URL/api/v1/oppgaver?statuskategori=AAPEN" +
                    "&tema=BAR&tema=KON" +
                    "&oppgavetype=JFR&oppgavetype=BEH_SAK" +
                    "&aktoerId=aktor-a&aktoerId=aktor-b" +
                    "&journalpostId=journal-a&journalpostId=journal-b" +
                    "&saksreferanse=sak%2Bverdi%26med%23tegn%7Bpar%7D" +
                    "&orgnr=orgnr-test" +
                    "&tildeltEnhetsnr=enhet-test" +
                    "&tilordnetRessurs=ressurs-test" +
                    "&behandlingstema=tema-test" +
                    "&behandlingstype=type-test" +
                    "&ferdigstiltFom=2026-04-03T05%3A06%3A07" +
                    "&ferdigstiltTom=2026-04-08T09%3A10%3A11" +
                    "&limit=1000&offset=17"
            server
                .expect(once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(CORRELATION_ID_HEADER, korrelasjonsId))
                .andRespond(
                    withSuccess(
                        """
                        {
                          "ukjentSokResponsefelt": true,
                          "antallTreffTotalt": 1,
                          "oppgaver": [$OPPGAVE_JSON]
                        }
                        """.trimIndent(),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            // Act
            val response = klient.finnOppgaver(request, korrelasjonsId)

            // Assert
            assertThat(response.antallTreffTotalt).isEqualTo(1)
            assertThat(response.oppgaver.orEmpty().map { it.id }).containsExactly(321)
        }

        @Test
        fun `skal utelate null og tomme lister men beholde tom streng i query-string og parse gyldig respons`() {
            // Arrange
            val request =
                FinnOppgaverRequest(
                    statuskategori = "",
                    tema = null,
                    oppgavetype = emptyList(),
                    tildeltEnhetsnr = "",
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver?statuskategori=&tildeltEnhetsnr="))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""{"antallTreffTotalt":0,"oppgaver":[]}""", MediaType.APPLICATION_JSON))

            // Act
            val response = klient.finnOppgaver(request)

            // Assert
            assertThat(response.antallTreffTotalt).isZero()
            assertThat(response.oppgaver.orEmpty()).isEmpty()
        }
    }

    @Nested
    inner class OpprettOppgave {
        @Test
        fun `skal opprette oppgave med påkrevde og utvalgte valgfrie felter`() {
            // Arrange
            val korrelasjonsId = "f6d8b63d-552c-4bf8-a7bd-22f0b4d62127"
            val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            val request =
                OpprettOppgaveRequest(
                    aktivDato = LocalDate.of(2026, 4, 3),
                    oppgavetype = "JFR",
                    prioritet = "NORM",
                    tema = "BAR",
                    personident = "syntetisk-personident",
                    journalpostId = "syntetisk-journalpost",
                    fristFerdigstillelse = LocalDate.of(2026, 5, 4),
                    uuid = uuid,
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(CORRELATION_ID_HEADER, korrelasjonsId))
                .andExpect(
                    content().json(
                        """
                        {
                          "aktivDato": "2026-04-03",
                          "oppgavetype": "JFR",
                          "prioritet": "NORM",
                          "tema": "BAR",
                          "personident": "syntetisk-personident",
                          "journalpostId": "syntetisk-journalpost",
                          "fristFerdigstillelse": "2026-05-04",
                          "uuid": "$uuid"
                        }
                        """.trimIndent(),
                        JsonCompareMode.STRICT,
                    ),
                ).andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(OPPGAVE_JSON))

            // Act
            val oppgave = klient.opprettOppgave(request, korrelasjonsId)

            // Assert
            assertThat(oppgave.id).isEqualTo(321)
            assertThat(oppgave.status).isEqualTo("NY_FREMTIDIG_STATUS")
        }
    }

    @Nested
    inner class PatchOppgave {
        @Test
        fun `skal ta med alle felter som er satt til Null eller Verdi i request`() {
            // Arrange
            val korrelasjonsId = "f6d8b63d-552c-4bf8-a7bd-22f0b4d62127"
            val request =
                PatchOppgaveRequest(
                    versjon = 4,
                    tilordnetRessurs = PatchFelt.Null,
                    status = PatchFelt.Verdi("AAPEN"),
                    beskrivelse = PatchFelt.Verdi(""),
                    aktivDato = PatchFelt.Verdi(LocalDate.of(2026, 4, 3)),
                    kommentar = PatchFelt.Verdi(RegistrerKommentarJson(tekst = "syntetisk kommentar", automatiskGenerert = false)),
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(CORRELATION_ID_HEADER, korrelasjonsId))
                .andExpect(
                    content().json(
                        """
                        {
                          "versjon": 4,
                          "tilordnetRessurs": null,
                          "status": "AAPEN",
                          "beskrivelse": "",
                          "aktivDato": "2026-04-03",
                          "kommentar": {
                            "tekst": "syntetisk kommentar",
                            "automatiskGenerert": false
                          }
                        }
                        """.trimIndent(),
                        JsonCompareMode.STRICT,
                    ),
                ).andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))

            // Act
            val oppgave = klient.patchOppgave(321, request, korrelasjonsId)

            // Assert
            assertThat(oppgave.id).isEqualTo(321)
            assertThat(oppgave.versjon).isEqualTo(4)
        }
    }

    private companion object {
        const val BASE_URL = "https://oppgave.test"
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        val OPPGAVE_JSON =
            """
            {
              "ukjentResponsfelt": true,
              "id": 321,
              "tildeltEnhetsnr": "enhet-test",
              "tema": "BAR",
              "oppgavetype": "JFR",
              "versjon": 4,
              "prioritet": "NY_FREMTIDIG_PRIORITET",
              "status": "NY_FREMTIDIG_STATUS",
              "aktivDato": "2026-04-03",
              "bruker": {
                "ident": "syntetisk-ident",
                "type": "NY_FREMTIDIG_BRUKERTYPE",
                "ukjentBrukerfelt": "verdi"
              }
            }
            """.trimIndent()
    }
}
