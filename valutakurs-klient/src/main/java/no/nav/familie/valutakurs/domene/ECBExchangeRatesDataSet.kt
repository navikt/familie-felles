package no.nav.familie.valutakurs.domene

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class ECBExchangeRatesDataSet(
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Series")
    val ecbExchangeRatesForCurrencies: List<ECBExchangeRatesForCurrency>
)
