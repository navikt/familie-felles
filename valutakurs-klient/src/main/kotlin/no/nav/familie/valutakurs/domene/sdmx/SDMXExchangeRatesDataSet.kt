package no.nav.familie.valutakurs.domene.sdmx

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class SDMXExchangeRatesDataSet(
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Series")
    val sdmxExchangeRatesForCurrencies: List<SDMXExchangeRatesForCurrency>,
)
