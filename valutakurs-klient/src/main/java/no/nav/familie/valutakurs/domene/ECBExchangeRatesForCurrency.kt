package no.nav.familie.valutakurs.domene

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class ECBExchangeRatesForCurrency(
    @field:JacksonXmlElementWrapper
    @field:JacksonXmlProperty(localName = "SeriesKey")
    val ecbExchangeRateKeys: List<ECBExchangeRateKey>,
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Obs")
    val ecbExchangeRates: List<ECBExchangeRate>,
)
