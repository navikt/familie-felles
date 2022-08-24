package no.nav.familie.http.ecb

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.http.ecb.domene.exchangeRateForCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBClientTest {

    lateinit var ecbRestClient: ECBRestClient

    @BeforeAll
    fun setup() {
        val mapper = XmlMapper()
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mapper.registerKotlinModule()
        val converter = MappingJackson2HttpMessageConverter(mapper)
        val mediaTypes = ArrayList<MediaType>()
        mediaTypes.addAll(converter.supportedMediaTypes)
        mediaTypes.add(MediaType.parseMediaType("application/vnd.sdmx.genericdata+xml;version=2.1"))
        converter.supportedMediaTypes = mediaTypes
        val restOperations = RestTemplateBuilder()
            .additionalMessageConverters(converter)
            .build()
        ecbRestClient = ECBRestClient(restOperations)
    }

    @Test
    fun `Test at ECBClient henter kurser for både SEK og NOK og at valutakursdatoen er korrekt`() {
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
    fun `Test at ECBClient henter kurs kun for NOK dersom utenlandskValuta er EUR`() {
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
}
