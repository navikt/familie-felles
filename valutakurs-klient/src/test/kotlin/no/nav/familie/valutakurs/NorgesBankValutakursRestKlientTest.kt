package no.nav.familie.valutakurs

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.valutakurs.SDMXRestKlient.Companion.APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA
import no.nav.familie.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.familie.valutakurs.domene.ecb.ECBValutakursData
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursData
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursDataSet
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursSeries
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateAttributes
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateDate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateKey
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateValue
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesForCurrency
import no.nav.familie.valutakurs.exception.NorgesBankValutakursMappingException
import no.nav.familie.valutakurs.exception.ValutakursClientException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NorgesBankValutakursRestKlientTest {
    private val xmlMapper = SDMXValutakursRestKlientConfig().xmlMapper()
    private val restTemplate = SDMXValutakursRestKlientConfig().xmlRestTemplate()
    private val norgesBankValutakursRestKlient: NorgesBankValutakursRestKlient
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    init {
        wireMockServer.start()
        norgesBankValutakursRestKlient = NorgesBankValutakursRestKlient(restTemplate, "http://localhost:${wireMockServer.port()}/")
    }

    @AfterAll
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun `Hente kurs for SEK for oppgitt valutakursdato og frekvens`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val valuta = "SEK"
        val kurs = BigDecimal.valueOf(10.6543)
        val body =
            lagNorgesBankValutakursDataXMLString(
                Frekvens.VIRKEDAG,
                Pair(valuta, kurs),
                valutakursDato.toString(),
            )
        wireMockServer.stubFor(
            WireMock
                .get("/B.$valuta.NOK.SP?format=sdmx-generic-2.1&startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                        .withStatus(200)
                        .withBody(body),
                ),
        )
        val valutakurs = norgesBankValutakursRestKlient.hentValutakurs(Frekvens.VIRKEDAG, valuta, valutakursDato)
        assertNotNull(valutakurs)
        assertEquals(valutakursDato, valutakurs.kursDato)
        assertEquals(valuta, valutakurs.valuta)
        assertEquals(kurs, valutakurs.kurs)
    }

    @Test
    fun `Hente kurs for DKK og sørge for at vi kurs konverteres til kurs per NOK`() {
        val valutakursDato = LocalDate.of(2022, 6, 29)
        val valuta = "DKK"
        val kursPerHundreNOK = BigDecimal.valueOf(138.54)
        val kursPerNOK = BigDecimal.valueOf(1.3854)
        val body =
            lagNorgesBankValutakursDataXMLString(
                frekvens = Frekvens.VIRKEDAG,
                valuta = Pair(valuta, kursPerHundreNOK),
                kursDato = valutakursDato.toString(),
                enhetsMultiplikator = "2",
            )
        wireMockServer.stubFor(
            WireMock
                .get("/B.$valuta.NOK.SP?format=sdmx-generic-2.1&startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                        .withStatus(200)
                        .withBody(body),
                ),
        )
        val valutakurs = norgesBankValutakursRestKlient.hentValutakurs(Frekvens.VIRKEDAG, valuta, valutakursDato)
        assertNotNull(valutakurs)
        assertEquals(valutakursDato, valutakurs.kursDato)
        assertEquals(valuta, valutakurs.valuta)
        assertEquals(kursPerNOK, valutakurs.kurs)
    }

    @Test
    fun `Skal kaste feil dersom respons fra Norges Bank er tom`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val valuta = "SEK"
        val body = ""
        wireMockServer.stubFor(
            WireMock
                .get("/B.$valuta.NOK.SP?format=sdmx-generic-2.1&startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                        .withStatus(200)
                        .withBody(body),
                ),
        )
        val valutakursClientException =
            assertThrows<ValutakursClientException> {
                norgesBankValutakursRestKlient.hentValutakurs(
                    Frekvens.VIRKEDAG,
                    valuta,
                    valutakursDato,
                )
            }
        assertTrue(valutakursClientException.cause is NullPointerException)
    }

    @Test
    fun `Skal kaste feil dersom respons fra Norges Bank gir statuskode ulik 200`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val valuta = "SEK"
        val body = ""
        wireMockServer.stubFor(
            WireMock
                .get("/B.$valuta.NOK.SP?format=sdmx-generic-2.1&startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                        .withStatus(400)
                        .withBody(body),
                ),
        )
        val valutakursClientException =
            assertThrows<ValutakursClientException> {
                norgesBankValutakursRestKlient.hentValutakurs(
                    Frekvens.VIRKEDAG,
                    valuta,
                    valutakursDato,
                )
            }
        assertTrue(valutakursClientException.cause is RestClientResponseException)
        assertEquals(HttpStatus.BAD_REQUEST.value(), (valutakursClientException.cause as RestClientResponseException).rawStatusCode)
    }

    @Test
    fun `Skal kaste feil dersom respons fra Norges Bank mangler nødvendige felter`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val valuta = "SEK"
        val body =
            lagUfullstendigNorgesBankValutakursDataXMLString(
                listOf(Pair(valuta, BigDecimal.valueOf(10.337))),
                YearMonth.of(2022, 7).toString(),
            )
        wireMockServer.stubFor(
            WireMock
                .get("/B.$valuta.NOK.SP?format=sdmx-generic-2.1&startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                        .withStatus(200)
                        .withBody(body),
                ),
        )
        val valutakursClientException =
            assertThrows<ValutakursClientException> {
                norgesBankValutakursRestKlient.hentValutakurs(
                    Frekvens.VIRKEDAG,
                    valuta,
                    valutakursDato,
                )
            }
        assertTrue(valutakursClientException.cause is NorgesBankValutakursMappingException.ManglerFelt)
    }

    private fun lagNorgesBankValutakursDataXMLString(
        frekvens: Frekvens,
        valuta: Pair<String, BigDecimal>,
        kursDato: String,
        enhetsMultiplikator: String = "0",
    ): String =
        xmlMapper.writeValueAsString(
            NorgesBankValutakursData(
                NorgesBankValutakursDataSet(
                    NorgesBankValutakursSeries(
                        listOf(
                            SDMXExchangeRateKey("BASE_CUR", valuta.first),
                            SDMXExchangeRateKey("FREQ", frekvens.verdi),
                        ),
                        listOf(
                            SDMXExchangeRateAttributes("UNIT_MULT", enhetsMultiplikator),
                            SDMXExchangeRateAttributes("CALCULATED", false.toString()),
                            SDMXExchangeRateAttributes("COLLECTION", "C"),
                        ),
                        SDMXExchangeRate(
                            SDMXExchangeRateDate(kursDato),
                            SDMXExchangeRateValue((valuta.second)),
                        ),
                    ),
                ),
            ),
        )

    private fun lagUfullstendigNorgesBankValutakursDataXMLString(
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): String =
        xmlMapper.writeValueAsString(
            ECBValutakursData(
                SDMXExchangeRatesDataSet(
                    exchangeRates.map {
                        SDMXExchangeRatesForCurrency(
                            listOf(),
                            listOf(),
                            listOf(
                                SDMXExchangeRate(
                                    SDMXExchangeRateDate(exchangeRateDate),
                                    SDMXExchangeRateValue((it.second)),
                                ),
                            ),
                        )
                    },
                ),
            ),
        )
}
