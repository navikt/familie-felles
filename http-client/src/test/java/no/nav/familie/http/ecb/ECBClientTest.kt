package no.nav.familie.http.ecb

import no.nav.familie.http.config.ECBRestTemplate
import no.nav.familie.http.ecb.domene.exchangeRateForCurrency
import no.nav.familie.http.interceptor.ECBRestClientInterceptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBClientTest {

    lateinit var ecbRestClient: ECBRestClient

    @BeforeAll
    fun setup() {
        val config = ECBRestTemplate()
        val restTemplate = config.xmlRestTemplate(ECBRestClientInterceptor(), config.xmlMapper())
        ecbRestClient = ECBRestClient(restTemplate, "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/")
    }

    @Test
    fun `Test at ECBRestClient henter kurser for både SEK og NOK og at valutakursdatoen er korrekt`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val valutakurser = ecbRestClient.getExchangeRates(Frequency.Daily, listOf("SEK", "NOK"), valutakursDato)
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
        val valutakurser = ecbRestClient.getExchangeRates(Frequency.Daily, listOf("NOK", "EUR"), valutakursDato)
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
        val valutakurser = ecbRestClient.getExchangeRates(Frequency.Monthly, listOf("NOK"), valutakursDato)
        val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
        assertEquals(YearMonth.of(2022, 6).atEndOfMonth(), nokValutakurs?.date)
    }

    @Test
    fun `Test at ECBRestClient henter kurs for inneværende måned dersom frekvens er Monthly og dato er siste i mnd`() {
        val valutakursDato = LocalDate.of(2022, 7, 31)
        val valutakurser = ecbRestClient.getExchangeRates(Frequency.Monthly, listOf("NOK"), valutakursDato)
        val nokValutakurs = valutakurser.exchangeRateForCurrency("NOK")
        assertEquals(YearMonth.of(2022, 7).atEndOfMonth(), nokValutakurs?.date)
    }
}
