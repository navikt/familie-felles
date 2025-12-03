package no.nav.familie.valutakurs

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.valutakurs.SDMXRestKlient.Companion.APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA
import no.nav.familie.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.familie.valutakurs.domene.ecb.ECBValutakursData
import no.nav.familie.valutakurs.domene.ecb.Frequency
import no.nav.familie.valutakurs.domene.exchangeRateForCurrency
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateDate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateKey
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateValue
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesForCurrency
import no.nav.familie.valutakurs.exception.ValutakursClientException
import no.nav.familie.valutakurs.exception.ValutakursTransformationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValutakursRestClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

        private lateinit var ecbValutakursRestKlient: ECBValutakursRestKlient
        private lateinit var xmlMapper: XmlMapper

        @BeforeAll
        @JvmStatic
        fun setup() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()

            val config = SDMXValutakursRestKlientConfig()
            xmlMapper = config.xmlMapper()
            val restTemplate = config.xmlRestTemplate()
            ecbValutakursRestKlient =
                ECBValutakursRestKlient(restTemplate, URI.create("http://localhost:${wireMockServer.port()}/").toString())
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }

    @Nested
    inner class ValutakursRestClientTest {
        @Test
        fun `Skal hente kurser for både SEK og NOK for oppgitt valutakursdato`() {
            val valutakursDato = LocalDate.of(2022, 7, 22)
            val body =
                createECBResponseBody(
                    Frequency.Daily,
                    listOf(Pair("SEK", BigDecimal.valueOf(10.6543)), Pair("NOK", BigDecimal.valueOf(10.337))),
                    valutakursDato.toString(),
                )
            wireMockServer.stubFor(
                WireMock
                    .get("/D.SEK+NOK.EUR.SP00.A?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                    .willReturn(
                        WireMock
                            .aResponse()
                            .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                            .withStatus(200)
                            .withBody(body),
                    ),
            )
            val valutakurser = ecbValutakursRestKlient.hentValutakurs(Frequency.Daily, listOf("SEK", "NOK"), valutakursDato)
            assertNotNull(valutakurser)
            assertEquals(2, valutakurser.size)
            val sekValutakurs = valutakurser.exchangeRateForCurrency("SEK")
            val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
            assertNotNull(sekValutakurs)
            assertNotNull(nokValutakurs)
            assertEquals(valutakursDato, sekValutakurs?.kursDato)
            assertEquals(valutakursDato, nokValutakurs?.kursDato)
        }

        @Test
        fun `Skal hente kurs kun for NOK dersom forespurte valutaer er NOK og EUR`() {
            val valutakursDato = LocalDate.of(2022, 7, 22)
            val body = createECBResponseBody(Frequency.Daily, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), valutakursDato.toString())
            wireMockServer.stubFor(
                WireMock
                    .get("/D.NOK+EUR.EUR.SP00.A?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                    .willReturn(
                        WireMock
                            .aResponse()
                            .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                            .withStatus(200)
                            .withBody(body),
                    ),
            )
            val valutakurser = ecbValutakursRestKlient.hentValutakurs(Frequency.Daily, listOf("NOK", "EUR"), valutakursDato)
            assertNotNull(valutakurser)
            assertEquals(1, valutakurser.size)
            val eurValutakurs = valutakurser.exchangeRateForCurrency("EUR")
            val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
            assertNull(eurValutakurs)
            assertNotNull(nokValutakurs)
            assertEquals(valutakursDato, nokValutakurs?.kursDato)
        }

        @Test
        fun `Skal hente kurs for forrige måned dersom frekvens er Monthly og dato ikke er siste i mnd`() {
            val valutakursDato = LocalDate.of(2022, 7, 22)
            val body =
                createECBResponseBody(Frequency.Monthly, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 6).toString())
            wireMockServer.stubFor(
                WireMock
                    .get("/M.NOK.EUR.SP00.A?endPeriod=$valutakursDato&lastNObservations=1")
                    .willReturn(
                        WireMock
                            .aResponse()
                            .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                            .withStatus(200)
                            .withBody(body),
                    ),
            )
            val valutakurser = ecbValutakursRestKlient.hentValutakurs(Frequency.Monthly, listOf("NOK"), valutakursDato)
            val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
            assertEquals(YearMonth.of(2022, 6).atEndOfMonth(), nokValutakurs?.kursDato)
        }

        @Test
        fun `Skal hente kurs for inneværende måned dersom frekvens er Monthly og dato er siste i mnd`() {
            val valutakursDato = LocalDate.of(2022, 7, 31)
            val body =
                createECBResponseBody(Frequency.Monthly, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 7).toString())
            wireMockServer.stubFor(
                WireMock
                    .get("/M.NOK.EUR.SP00.A?endPeriod=$valutakursDato&lastNObservations=1")
                    .willReturn(
                        WireMock
                            .aResponse()
                            .withHeader("Content-Type", APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
                            .withStatus(200)
                            .withBody(body),
                    ),
            )
            val valutakurser = ecbValutakursRestKlient.hentValutakurs(Frequency.Monthly, listOf("NOK"), valutakursDato)
            val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
            assertEquals(YearMonth.of(2022, 7).atEndOfMonth(), nokValutakurs?.kursDato)
        }

        @Test
        fun `Skal kaste feil dersom respons fra ECB er tom`() {
            val valutakursDato = LocalDate.of(2022, 7, 31)
            val body = ""
            wireMockServer.stubFor(
                WireMock
                    .get("/D.NOK+SEK.EUR.SP00.A?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
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
                    ecbValutakursRestKlient.hentValutakurs(
                        Frequency.Daily,
                        listOf("NOK", "SEK"),
                        valutakursDato,
                    )
                }
            assertTrue(valutakursClientException.cause is NullPointerException)
        }

        @Test
        fun `Skal kaste feil dersom respons fra ECB gir statuskode ulik 200`() {
            val valutakursDato = LocalDate.of(2022, 7, 31)
            val body = ""
            wireMockServer.stubFor(
                WireMock
                    .get("/D.NOK+SEK.EUR.SP00.A?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
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
                    ecbValutakursRestKlient.hentValutakurs(
                        Frequency.Daily,
                        listOf("NOK", "SEK"),
                        valutakursDato,
                    )
                }
            assertTrue(valutakursClientException.cause is RestClientResponseException)
            assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                (valutakursClientException.cause as RestClientResponseException).statusCode.value(),
            )
        }

        @Test
        fun `Skal kaste feil dersom respons fra ECB mangler nødvendige felter`() {
            val valutakursDato = LocalDate.of(2022, 7, 31)
            val body = createIncompleteResponseBody(listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 7).toString())
            wireMockServer.stubFor(
                WireMock
                    .get("/D.NOK.EUR.SP00.A?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
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
                    ecbValutakursRestKlient.hentValutakurs(
                        Frequency.Daily,
                        listOf("NOK"),
                        valutakursDato,
                    )
                }
            assertTrue(valutakursClientException.cause is ValutakursTransformationException)
            assertTrue((valutakursClientException.cause as ValutakursTransformationException).cause is NoSuchElementException)
        }

        @Test
        fun `Skal kaste feil dersom respons fra ECB har feil datoformat`() {
            val valutakursDato = LocalDate.of(2022, 7, 31)
            val body =
                createECBResponseBody(Frequency.Daily, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 7).toString())
            wireMockServer.stubFor(
                WireMock
                    .get("/D.NOK.EUR.SP00.A?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
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
                    ecbValutakursRestKlient.hentValutakurs(
                        Frequency.Daily,
                        listOf("NOK"),
                        valutakursDato,
                    )
                }
            assertTrue(valutakursClientException.cause is ValutakursTransformationException)
            assertTrue((valutakursClientException.cause as ValutakursTransformationException).cause is DateTimeParseException)
        }
    }

    private fun createECBResponseBody(
        frequency: Frequency,
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): String =
        xmlMapper.writeValueAsString(
            ECBValutakursData(
                SDMXExchangeRatesDataSet(
                    exchangeRates.map {
                        SDMXExchangeRatesForCurrency(
                            listOf(SDMXExchangeRateKey("CURRENCY", it.first), SDMXExchangeRateKey("FREQ", frequency.toFrequencyParam())),
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

    private fun createIncompleteResponseBody(
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): String =
        xmlMapper.writeValueAsString(
            ECBValutakursData(
                SDMXExchangeRatesDataSet(
                    exchangeRates.map {
                        SDMXExchangeRatesForCurrency(
                            listOf(SDMXExchangeRateKey("", "")),
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
