package no.nav.familie.valutakurs.domene.sdmx

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class SDMXExchangeRatesForCurrency(
    @field:JacksonXmlElementWrapper
    @field:JacksonXmlProperty(localName = "SeriesKey")
    val sdmxExchangeRateKeys: List<SDMXExchangeRateKey>,
    @field:JacksonXmlElementWrapper
    @field:JacksonXmlProperty(localName = "Attributes")
    val sdmxExchangeRateAttributes: List<SDMXExchangeRateAttributes>,
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Obs")
    val sdmxExchangeRates: List<SDMXExchangeRate>,
)
