package no.nav.familie.valutakurs.domene.sdmx

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class SDMXExchangeRatesDataSet(
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Series")
    val sdmxExchangeRatesForCurrencies: List<SDMXExchangeRatesForCurrency>,
)
