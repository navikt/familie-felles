package no.nav.familie.valutakurs

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.valutakurs.config.ValutakursRestClientConfig
import no.nav.familie.valutakurs.domene.ECBExchangeRate
import no.nav.familie.valutakurs.domene.ECBExchangeRateDate
import no.nav.familie.valutakurs.domene.ECBExchangeRateKey
import no.nav.familie.valutakurs.domene.ECBExchangeRateValue
import no.nav.familie.valutakurs.domene.ECBExchangeRatesData
import no.nav.familie.valutakurs.domene.ECBExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.ECBExchangeRatesForCurrency
import no.nav.familie.valutakurs.domene.exchangeRateForCurrency
import no.nav.familie.valutakurs.exception.ValutakursClientException
import no.nav.familie.valutakurs.exception.ValutakursTransformationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValutakursRestClientTest {
    private val contentType = "application/vnd.sdmx.genericdata+xml;version=2.1"

    companion object {
        private lateinit var wireMockServer: WireMockServer

        private lateinit var valutakursRestClient: ValutakursRestClient
        private lateinit var xmlMapper: XmlMapper

        @BeforeAll
        @JvmStatic
        fun setup() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()

            val config = ValutakursRestClientConfig()
            xmlMapper = config.xmlMapper()
            val restTemplate = config.xmlRestTemplate()
            valutakursRestClient = ValutakursRestClient(restTemplate, URI.create("http://localhost:${wireMockServer.port()}/").toString())
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }

    @Test
    fun `Test at ECBRestClient henter kurser for både SEK og NOK og at valutakursdatoen er korrekt`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val body =
            createECBResponseBody(
                Frequency.Daily,
                listOf(Pair("SEK", BigDecimal.valueOf(10.6543)), Pair("NOK", BigDecimal.valueOf(10.337))),
                valutakursDato.toString(),
            )
        wireMockServer.stubFor(
            WireMock.get("/D.SEK+NOK.EUR.SP00.A/?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(200).withBody(body)),
        )
        val valutakurser = valutakursRestClient.hentValutakurs(Frequency.Daily, listOf("SEK", "NOK"), valutakursDato)
        assertNotNull(valutakurser)
        assertEquals(2, valutakurser.size)
        val sekValutakurs = valutakurser.exchangeRateForCurrency("SEK")
        val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
        assertNotNull(sekValutakurs)
        assertNotNull(nokValutakurs)
        assertEquals(valutakursDato, sekValutakurs?.date)
        assertEquals(valutakursDato, nokValutakurs?.date)
    }

    @Test
    fun `Test at ECBRestClient henter kurs kun for NOK dersom forespurte valutaer er NOK og EUR`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val body = createECBResponseBody(Frequency.Daily, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), valutakursDato.toString())
        wireMockServer.stubFor(
            WireMock.get("/D.NOK+EUR.EUR.SP00.A/?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(200).withBody(body)),
        )
        val valutakurser = valutakursRestClient.hentValutakurs(Frequency.Daily, listOf("NOK", "EUR"), valutakursDato)
        assertNotNull(valutakurser)
        assertEquals(1, valutakurser.size)
        val eurValutakurs = valutakurser.exchangeRateForCurrency("EUR")
        val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
        assertNull(eurValutakurs)
        assertNotNull(nokValutakurs)
        assertEquals(valutakursDato, nokValutakurs?.date)
    }

    @Test
    fun `Test at ECBRestClient henter kurs for forrige måned dersom frekvens er Monthly og dato ikke er siste i mnd`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val body =
            createECBResponseBody(Frequency.Monthly, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 6).toString())
        wireMockServer.stubFor(
            WireMock.get("/M.NOK.EUR.SP00.A/?endPeriod=$valutakursDato&lastNObservations=1")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(200).withBody(body)),
        )
        val valutakurser = valutakursRestClient.hentValutakurs(Frequency.Monthly, listOf("NOK"), valutakursDato)
        val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
        assertEquals(YearMonth.of(2022, 6).atEndOfMonth(), nokValutakurs?.date)
    }

    @Test
    fun `Test at ECBRestClient henter kurs for inneværende måned dersom frekvens er Monthly og dato er siste i mnd`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val body =
            createECBResponseBody(Frequency.Monthly, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 7).toString())
        wireMockServer.stubFor(
            WireMock.get("/M.NOK.EUR.SP00.A/?endPeriod=$valutakursDato&lastNObservations=1")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(200).withBody(body)),
        )
        val valutakurser = valutakursRestClient.hentValutakurs(Frequency.Monthly, listOf("NOK"), valutakursDato)
        val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
        assertEquals(YearMonth.of(2022, 7).atEndOfMonth(), nokValutakurs?.date)
    }

    @Test
    fun `Test at ECBRestClient kaster feil dersom respons fra ECB er tom`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val body = ""
        wireMockServer.stubFor(
            WireMock.get("/D.NOK+SEK.EUR.SP00.A/?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(200).withBody(body)),
        )
        val valutakursClientException =
            assertThrows<ValutakursClientException> {
                valutakursRestClient.hentValutakurs(
                    Frequency.Daily,
                    listOf("NOK", "SEK"),
                    valutakursDato,
                )
            }

        assertTrue(valutakursClientException.cause?.cause is HttpMessageNotReadableException)
    }

    @Test
    fun `Test at ECBRestClient kaster feil dersom respons fra ECB gir statuskode ulik 200`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val body = ""
        wireMockServer.stubFor(
            WireMock.get("/D.NOK+SEK.EUR.SP00.A/?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(400).withBody(body)),
        )
        val valutakursClientException =
            assertThrows<ValutakursClientException> {
                valutakursRestClient.hentValutakurs(
                    Frequency.Daily,
                    listOf("NOK", "SEK"),
                    valutakursDato,
                )
            }
        assertTrue(valutakursClientException.cause is RestClientResponseException)
        assertEquals(HttpStatus.BAD_REQUEST.value(), (valutakursClientException.cause as RestClientResponseException).rawStatusCode)
    }

    @Test
    fun `Test at ECBRestClient kaster feil dersom respons fra ECB mangler nødvendige felter`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val body = createIncompleteECBResponseBody(listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 7).toString())
        wireMockServer.stubFor(
            WireMock.get("/D.NOK.EUR.SP00.A/?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(200).withBody(body)),
        )
        val valutakursClientException =
            assertThrows<ValutakursClientException> { valutakursRestClient.hentValutakurs(Frequency.Daily, listOf("NOK"), valutakursDato) }
        assertTrue(valutakursClientException.cause is ValutakursTransformationException)
        assertTrue((valutakursClientException.cause as ValutakursTransformationException).cause is NoSuchElementException)
    }

    @Test
    fun `Test at ECBRestClient kaster feil dersom respons fra ECB har feil datoformat`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val body = createECBResponseBody(Frequency.Daily, listOf(Pair("NOK", BigDecimal.valueOf(10.337))), YearMonth.of(2022, 7).toString())
        wireMockServer.stubFor(
            WireMock.get("/D.NOK.EUR.SP00.A/?startPeriod=$valutakursDato&endPeriod=$valutakursDato")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", contentType).withStatus(200).withBody(body)),
        )
        val valutakursClientException =
            assertThrows<ValutakursClientException> { valutakursRestClient.hentValutakurs(Frequency.Daily, listOf("NOK"), valutakursDato) }
        assertTrue(valutakursClientException.cause is ValutakursTransformationException)
        assertTrue((valutakursClientException.cause as ValutakursTransformationException).cause is DateTimeParseException)
    }

    private fun createECBResponseBody(
        frequency: Frequency,
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): String {
        return xmlMapper.writeValueAsString(
            ECBExchangeRatesData(
                ECBExchangeRatesDataSet(
                    exchangeRates.map {
                        ECBExchangeRatesForCurrency(
                            listOf(ECBExchangeRateKey("CURRENCY", it.first), ECBExchangeRateKey("FREQ", frequency.toFrequencyParam())),
                            listOf(
                                ECBExchangeRate(
                                    ECBExchangeRateDate(exchangeRateDate),
                                    ECBExchangeRateValue((it.second)),
                                ),
                            ),
                        )
                    },
                ),
            ),
        )
    }

    private fun createIncompleteECBResponseBody(
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): String {
        return xmlMapper.writeValueAsString(
            ECBExchangeRatesData(
                ECBExchangeRatesDataSet(
                    exchangeRates.map {
                        ECBExchangeRatesForCurrency(
                            listOf(ECBExchangeRateKey("", "")),
                            listOf(
                                ECBExchangeRate(
                                    ECBExchangeRateDate(exchangeRateDate),
                                    ECBExchangeRateValue((it.second)),
                                ),
                            ),
                        )
                    },
                ),
            ),
        )
    }
}
