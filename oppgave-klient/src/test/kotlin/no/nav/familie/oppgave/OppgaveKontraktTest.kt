package no.nav.familie.oppgave

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.oppgave.modell.Brukertype
import no.nav.familie.oppgave.modell.FinnOppgaverRequest
import no.nav.familie.oppgave.modell.MetadataKey
import no.nav.familie.oppgave.modell.Oppgavestatus
import no.nav.familie.oppgave.modell.Oppgavestatuskategori
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import no.nav.familie.oppgave.modell.PatchFelt
import no.nav.familie.oppgave.modell.PatchOppgaveRequest
import no.nav.familie.oppgave.modell.Prioritet
import no.nav.familie.oppgave.modell.RegistrerKommentar
import no.nav.familie.oppgave.modell.Sorteringsfelt
import no.nav.familie.oppgave.modell.Sorteringsrekkefolge
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.observation.ClientRequestObservationContext
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OppgaveKontraktTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val klient = OppgaveMaskinKlient(URI.create("$BASE_URL/"), builder.build())
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
            assertThat(oppgave.status).isEqualTo(Oppgavestatus.OPPRETTET)
            assertThat(oppgave.prioritet).isEqualTo(Prioritet.NORM)
            assertThat(oppgave.bruker?.ident).isEqualTo("syntetisk-ident")
            assertThat(oppgave.bruker?.type).isEqualTo(Brukertype.PERSON)
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
                    statuskategori = Oppgavestatuskategori.AAPEN,
                    tema = listOf("BAR", "KON"),
                    oppgavetype = listOf("JFR", "BEH_SAK"),
                    aktoerId = listOf("0000000000000", "1111111111111"),
                    journalpostId = listOf("journal-a", "journal-b"),
                    saksreferanse = listOf("sak+verdi&med#tegn{par}"),
                    orgnr = listOf("000000000"),
                    tildeltEnhetsnr = "1234",
                    tilordnetRessurs = "ABC1234",
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
                    "&aktoerId=0000000000000&aktoerId=1111111111111" +
                    "&journalpostId=journal-a&journalpostId=journal-b" +
                    "&saksreferanse=sak%2Bverdi%26med%23tegn%7Bpar%7D" +
                    "&orgnr=000000000" +
                    "&tildeltEnhetsnr=1234" +
                    "&tilordnetRessurs=ABC1234" +
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
                    statuskategori = Oppgavestatuskategori.AAPEN,
                    tema = null,
                    oppgavetype = emptyList(),
                    tildeltEnhetsnr = "",
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver?statuskategori=AAPEN&tildeltEnhetsnr="))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""{"antallTreffTotalt":0,"oppgaver":[]}""", MediaType.APPLICATION_JSON))

            // Act
            val response = klient.finnOppgaver(request)

            // Assert
            assertThat(response.antallTreffTotalt).isZero()
            assertThat(response.oppgaver.orEmpty()).isEmpty()
        }

        @Test
        fun `tomt søk og limit over 1000 sendes til API-et`() {
            // Arrange
            val tomtSok = FinnOppgaverRequest()
            server
                .expect(once()) { actual ->
                    assertThat(actual.uri.path).isEqualTo("/api/v1/oppgaver")
                    assertThat(actual.uri.rawQuery).isNull()
                }.andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""{"antallTreffTotalt":0,"oppgaver":[]}""", MediaType.APPLICATION_JSON))
            server
                .expect(once()) { actual ->
                    assertThat(actual.uri.path).isEqualTo("/api/v1/oppgaver")
                }.andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("limit", "1001"))
                .andRespond(withSuccess("""{"antallTreffTotalt":0,"oppgaver":[]}""", MediaType.APPLICATION_JSON))

            // Act
            val tomtSokResponse = klient.finnOppgaver(tomtSok)
            val overLimitResponse = klient.finnOppgaver(FinnOppgaverRequest(limit = 1001))

            // Assert
            assertThat(tomtSokResponse.antallTreffTotalt).isZero()
            assertThat(tomtSokResponse.oppgaver.orEmpty()).isEmpty()
            assertThat(overLimitResponse.antallTreffTotalt).isZero()
            assertThat(overLimitResponse.oppgaver.orEmpty()).isEmpty()
        }

        @Test
        fun `skal serialisere alle nye sokefiltre korrekt uten filterverdier i uri-malen`() {
            // Arrange
            val syntetiskVerdi = "syntetisk+verdi/med?sporsmal:tegn"
            val registry = ObservationRegistry.create()
            val uriMaler = mutableListOf<String?>()
            registry.observationConfig().observationHandler(
                object : ObservationHandler<ClientRequestObservationContext> {
                    override fun supportsContext(context: Observation.Context): Boolean = context is ClientRequestObservationContext

                    override fun onStart(context: ClientRequestObservationContext) {
                        uriMaler.add(context.uriTemplate)
                    }
                },
            )
            val observertBuilder = RestClient.builder().observationRegistry(registry)
            val observertServer = MockRestServiceServer.bindTo(observertBuilder).build()
            val observertKlient = OppgaveMaskinKlient(URI.create("$BASE_URL/"), observertBuilder.build())
            val request =
                FinnOppgaverRequest(
                    statuskategori = Oppgavestatuskategori.AAPEN,
                    status = listOf(Oppgavestatus.OPPRETTET, Oppgavestatus.AAPNET),
                    tildeltRessurs = false,
                    erUtenMappe = false,
                    mappeId = listOf(101L, 202L),
                    opprettetAv = syntetiskVerdi,
                    opprettetAvEnhetsnr = "4321",
                    behandlesAvApplikasjon = "syntetisk-app",
                    aktivDatoFom = LocalDate.of(2026, 4, 3),
                    aktivDatoTom = LocalDate.of(2026, 4, 8),
                    opprettetFom = LocalDateTime.of(2026, 4, 3, 5, 6, 7, 123_456_789),
                    opprettetTom = LocalDateTime.of(2026, 4, 8, 9, 10, 11, 123_456_789),
                    fristFom = LocalDate.of(2026, 4, 9),
                    fristTom = LocalDate.of(2026, 4, 10),
                    bnr = listOf("syntetisk-bnr-a", "syntetisk-bnr-b"),
                    samhandlernr = listOf("syntetisk-samhandler-a", "syntetisk-samhandler-b"),
                    metadatanokkel = MetadataKey.NORM_DATO,
                    metadataverdi = listOf("2026-04-03", "2026-04-08"),
                    sorteringsrekkefolge = Sorteringsrekkefolge.DESC,
                    sorteringsfelt = Sorteringsfelt.FRIST,
                    presisMatchTemakategorisering = false,
                )
            observertServer
                .expect(once()) { actual ->
                    assertThat(actual.uri.path).isEqualTo("/api/v1/oppgaver")
                }.andExpect(method(HttpMethod.GET))
                .andExpect {
                    val decodedQuery = decodeQuery(it.uri.rawQuery)
                    assertThat(decodedQuery)
                        .containsExactlyInAnyOrderEntriesOf(
                            mapOf(
                                "statuskategori" to listOf("AAPEN"),
                                "status" to listOf("OPPRETTET", "AAPNET"),
                                "tildeltRessurs" to listOf("false"),
                                "erUtenMappe" to listOf("false"),
                                "mappeId" to listOf("101", "202"),
                                "opprettetAv" to listOf(syntetiskVerdi),
                                "opprettetAvEnhetsnr" to listOf("4321"),
                                "behandlesAvApplikasjon" to listOf("syntetisk-app"),
                                "aktivDatoFom" to listOf("2026-04-03"),
                                "aktivDatoTom" to listOf("2026-04-08"),
                                "opprettetFom" to listOf("2026-04-03T05:06:07.123456789"),
                                "opprettetTom" to listOf("2026-04-08T09:10:11.123456789"),
                                "fristFom" to listOf("2026-04-09"),
                                "fristTom" to listOf("2026-04-10"),
                                "bnr" to listOf("syntetisk-bnr-a", "syntetisk-bnr-b"),
                                "samhandlernr" to listOf("syntetisk-samhandler-a", "syntetisk-samhandler-b"),
                                "metadatanokkel" to listOf("NORM_DATO"),
                                "metadataverdi" to listOf("2026-04-03", "2026-04-08"),
                                "sorteringsrekkefolge" to listOf("DESC"),
                                "sorteringsfelt" to listOf("FRIST"),
                                "presisMatchTemakategorisering" to listOf("false"),
                            ),
                        )
                    assertThat(it.uri.rawQuery)
                        .contains("syntetisk%2Bverdi%2Fmed%3Fsporsmal%3Ategn")
                        .doesNotContain(syntetiskVerdi)
                }.andRespond(withSuccess("""{"antallTreffTotalt":0,"oppgaver":[]}""", MediaType.APPLICATION_JSON))

            // Act
            val response = observertKlient.finnOppgaver(request)

            // Assert
            observertServer.verify()
            assertThat(response.antallTreffTotalt).isZero()
            assertThat(uriMaler).hasSize(1)
            assertThat(checkNotNull(uriMaler.single()))
                .contains("{query")
                .doesNotContain(syntetiskVerdi, "syntetisk%2Bverdi")
        }
    }

    @Nested
    inner class OpprettOppgave {
        @Test
        fun `sender alle POST-felter uten lokal validering`() {
            // Arrange
            val korrelasjonsId = "f6d8b63d-552c-4bf8-a7bd-22f0b4d62127"
            val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            val metadata = MetadataKey.entries.associateWith { "metadata-${it.name}" }
            val request =
                OpprettOppgaveRequest(
                    aktivDato = LocalDate.of(2026, 4, 3),
                    oppgavetype = "JFR",
                    prioritet = Prioritet.NORM,
                    tema = " ",
                    aktoerId = "0000000000000",
                    personident = "00000000000",
                    orgnr = "000000000",
                    bnr = "syntetisk-bnr",
                    samhandlernr = "syntetisk-samhandler",
                    tildeltEnhetsnr = "1234",
                    opprettetAvEnhetsnr = "4321",
                    journalpostId = "syntetisk-journalpost",
                    journalpostkilde = "syntetisk-kilde",
                    tilordnetRessurs = "ABC1234",
                    behandlesAvApplikasjon = "syntetisk-app",
                    saksreferanse = "syntetisk-sak",
                    beskrivelse = "",
                    temagruppe = "syntetisk-gruppe",
                    mappeId = 987L,
                    metadata = metadata,
                    behandlingstema = "syntetisk-behandlingstema",
                    behandlingstype = "syntetisk-behandlingstype",
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
                          "tema": " ",
                          "aktoerId": "0000000000000",
                          "personident": "00000000000",
                          "orgnr": "000000000",
                          "bnr": "syntetisk-bnr",
                          "samhandlernr": "syntetisk-samhandler",
                          "tildeltEnhetsnr": "1234",
                          "opprettetAvEnhetsnr": "4321",
                          "journalpostId": "syntetisk-journalpost",
                          "journalpostkilde": "syntetisk-kilde",
                          "tilordnetRessurs": "ABC1234",
                          "behandlesAvApplikasjon": "syntetisk-app",
                          "saksreferanse": "syntetisk-sak",
                          "beskrivelse": "",
                          "temagruppe": "syntetisk-gruppe",
                          "mappeId": 987,
                          "metadata": {
                            "NORM_DATO": "metadata-NORM_DATO",
                            "SOKNAD_ID": "metadata-SOKNAD_ID",
                            "REVURDERINGSTYPE": "metadata-REVURDERINGSTYPE",
                            "KRAV_ID": "metadata-KRAV_ID",
                            "MOTTATT_DATO": "metadata-MOTTATT_DATO",
                            "EKSTERN_HENVENDELSE_ID": "metadata-EKSTERN_HENVENDELSE_ID",
                            "SKANNET_DATO": "metadata-SKANNET_DATO",
                            "RINA_SAKID": "metadata-RINA_SAKID",
                            "HJEMMEL": "metadata-HJEMMEL",
                            "EKSTERN_ID": "metadata-EKSTERN_ID",
                            "BEHANDLING_ID": "metadata-BEHANDLING_ID"
                          },
                          "behandlingstema": "syntetisk-behandlingstema",
                          "behandlingstype": "syntetisk-behandlingstype",
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
            assertThat(oppgave.status).isEqualTo(Oppgavestatus.OPPRETTET)
        }

        @Test
        fun `POST 409 med oppgaveId henter eksisterende oppgave og gjenbruker korrelasjonsid`() {
            // Arrange
            val requestUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            val request =
                OpprettOppgaveRequest(
                    aktivDato = LocalDate.of(2026, 4, 3),
                    oppgavetype = "JFR",
                    prioritet = Prioritet.NORM,
                    tema = "BAR",
                    uuid = requestUuid,
                )
            var postKorrelasjonsId: String? = null
            var getKorrelasjonsId: String? = null
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                    content().json(
                        """
                        {
                          "aktivDato": "2026-04-03",
                          "oppgavetype": "JFR",
                          "prioritet": "NORM",
                          "tema": "BAR",
                          "uuid": "$requestUuid"
                        }
                        """.trimIndent(),
                        JsonCompareMode.STRICT,
                    ),
                ).andExpect {
                    postKorrelasjonsId = it.headers.getFirst(CORRELATION_ID_HEADER)
                }.andRespond(
                    withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""{"uuid":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","feilmelding":"duplikat","oppgaveId":321}"""),
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect {
                    getKorrelasjonsId = it.headers.getFirst(CORRELATION_ID_HEADER)
                }.andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))

            // Act
            val oppgave = klient.opprettOppgave(request)

            // Assert
            assertThat(oppgave.id).isEqualTo(321)
            assertThat(oppgave.status).isEqualTo(Oppgavestatus.OPPRETTET)
            assertThat(postKorrelasjonsId).isNotNull()
            assertThat(getKorrelasjonsId).isEqualTo(postKorrelasjonsId)
            assertThat(UUID.fromString(checkNotNull(postKorrelasjonsId)).toString()).isEqualTo(postKorrelasjonsId)
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "{}",
                "{",
                "{\"oppgaveId\":\"321\"}",
                "{\"oppgaveId\":321.5}",
                "{\"oppgaveId\":0}",
                "{\"oppgaveId\":9223372036854775808}",
            ],
        )
        fun `POST 409 uten gyldig oppgaveId gir opprinnelig HTTP-feil uten gjenfinning`(body: String) {
            // Arrange
            val korrelasjonsId = "gjenfinnings-call-id"
            val request =
                OpprettOppgaveRequest(
                    aktivDato = LocalDate.of(2026, 4, 3),
                    oppgavetype = "JFR",
                    prioritet = Prioritet.NORM,
                    tema = "BAR",
                    uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(body))

            // Act
            val feil = assertThrows<OppgaveException> { klient.opprettOppgave(request, korrelasjonsId) }

            // Assert
            assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.HTTP)
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.CONFLICT.value())
            assertThat(feil.operasjon).isEqualTo(OppgaveOperasjon.OPPRETT_OPPGAVE)
            assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
            assertThat((feil.cause as RestClientResponseException).responseBodyAsString).isEqualTo(body)
        }

        @ParameterizedTest
        @ValueSource(ints = [403, 500])
        fun `GET-feil etter POST 409 gir HTTP-feil fra gjenfinning`(statusCode: Int) {
            // Arrange
            val korrelasjonsId = "gjenfinnings-call-id"
            val getBody = """{"feilmelding":"syntetisk GET-feil"}"""
            val request =
                OpprettOppgaveRequest(
                    aktivDato = LocalDate.of(2026, 4, 3),
                    oppgavetype = "JFR",
                    prioritet = Prioritet.NORM,
                    tema = "BAR",
                    uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(CORRELATION_ID_HEADER, korrelasjonsId))
                .andRespond(
                    withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""{"uuid":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","feilmelding":"duplikat","oppgaveId":321}"""),
                )
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(CORRELATION_ID_HEADER, korrelasjonsId))
                .andRespond(withStatus(HttpStatus.valueOf(statusCode)).contentType(MediaType.APPLICATION_JSON).body(getBody))

            // Act
            val feil =
                assertThrows<OppgaveException> {
                    klient.opprettOppgave(request, korrelasjonsId)
                }

            // Assert
            assertThat(feil.feiltype).isEqualTo(OppgaveFeiltype.HTTP)
            assertThat(feil.httpStatus).isEqualTo(statusCode)
            assertThat(feil.operasjon).isEqualTo(OppgaveOperasjon.HENT_OPPGAVE)
            assertThat(feil.korrelasjonsId).isEqualTo(korrelasjonsId)
            assertThat(feil.message).isEqualTo("syntetisk GET-feil")
            assertThat(feil.cause).isInstanceOf(RestClientResponseException::class.java)
            val original = feil.cause as RestClientResponseException
            assertThat(original.statusCode.value()).isEqualTo(statusCode)
            assertThat(original.responseBodyAsString).isEqualTo(getBody)
        }
    }

    @Nested
    inner class PatchOppgave {
        @Test
        fun `PATCH sender Null i påkrevd felt og for langt tema uten lokal validering`() {
            // Arrange
            val korrelasjonsId = "f6d8b63d-552c-4bf8-a7bd-22f0b4d62127"
            val request =
                PatchOppgaveRequest(
                    versjon = 4,
                    tilordnetRessurs = PatchFelt.Null,
                    status = PatchFelt.Null,
                    orgnr = PatchFelt.Null,
                    tema = PatchFelt.Verdi("T".repeat(256)),
                    beskrivelse = PatchFelt.Verdi(""),
                    aktivDato = PatchFelt.Verdi(LocalDate.of(2026, 4, 3)),
                    kommentar = PatchFelt.Verdi(RegistrerKommentar(tekst = "syntetisk kommentar", automatiskGenerert = false)),
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
                          "status": null,
                          "orgnr": null,
                          "tema": "${"T".repeat(256)}",
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

    @Test
    fun `skriver ikke requestfelter i DEBUG logging ved opprettelse og patch`() {
        // Arrange
        val opprettPersonident = "00000000000"
        val opprettBeskrivelse = "syntetisk-opprett-beskrivelse-markor"
        val patchPersonident = "11111111111"
        val patchBeskrivelse = "syntetisk-patch-beskrivelse-markor"
        val patchKommentar = "syntetisk-patch-kommentar-markor"
        val opprettUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val logger = LoggerFactory.getLogger("org.springframework.web.client.DefaultRestClient") as Logger
        val opprinneligNiva = logger.level
        val appender =
            ListAppender<ILoggingEvent>().apply {
                context = logger.loggerContext
                start()
            }
        try {
            logger.level = Level.DEBUG
            logger.addAppender(appender)
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                    content().json(
                        """
                        {
                          "aktivDato": "2026-04-03",
                          "oppgavetype": "JFR",
                          "prioritet": "NORM",
                          "tema": "BAR",
                          "personident": "$opprettPersonident",
                          "beskrivelse": "$opprettBeskrivelse",
                          "uuid": "$opprettUuid"
                        }
                        """.trimIndent(),
                        JsonCompareMode.STRICT,
                    ),
                ).andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(OPPGAVE_JSON))
            server
                .expect(once(), requestTo("$BASE_URL/api/v1/oppgaver/321"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(
                    content().json(
                        """
                        {
                          "versjon": 4,
                          "beskrivelse": "$patchBeskrivelse",
                          "personident": "$patchPersonident",
                          "kommentar": {
                            "tekst": "$patchKommentar",
                            "automatiskGenerert": false
                          }
                        }
                        """.trimIndent(),
                        JsonCompareMode.STRICT,
                    ),
                ).andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))

            // Act
            klient.opprettOppgave(
                OpprettOppgaveRequest(
                    aktivDato = LocalDate.of(2026, 4, 3),
                    oppgavetype = "JFR",
                    prioritet = Prioritet.NORM,
                    tema = "BAR",
                    uuid = opprettUuid,
                    personident = opprettPersonident,
                    beskrivelse = opprettBeskrivelse,
                ),
            )
            klient.patchOppgave(
                321,
                PatchOppgaveRequest(
                    versjon = 4,
                    beskrivelse = PatchFelt.Verdi(patchBeskrivelse),
                    personident = PatchFelt.Verdi(patchPersonident),
                    kommentar = PatchFelt.Verdi(RegistrerKommentar(tekst = patchKommentar, automatiskGenerert = false)),
                ),
            )

            // Assert
            val loggOutput = appender.list.joinToString("\n") { it.formattedMessage }
            assertThat(loggOutput).contains("Writing [OpprettOppgaveRequest]")
            assertThat(loggOutput).contains("Writing [PatchOppgaveRequest]")
            assertThat(loggOutput)
                .doesNotContain(opprettPersonident, opprettBeskrivelse, patchPersonident, patchBeskrivelse, patchKommentar)
        } finally {
            logger.detachAppender(appender)
            logger.level = opprinneligNiva
            appender.stop()
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
              "prioritet": "NORM",
              "status": "OPPRETTET",
              "aktivDato": "2026-04-03",
              "bruker": {
                "ident": "syntetisk-ident",
                "type": "PERSON",
                "ukjentBrukerfelt": "verdi"
              }
            }
            """.trimIndent()
    }

    private fun decodeQuery(rawQuery: String): Map<String, List<String>> =
        rawQuery
            .split('&')
            .map { parameter ->
                val (key, value) = parameter.split('=', limit = 2)
                URLDecoder.decode(key, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
            }.groupBy({ it.first }, { it.second })
}
