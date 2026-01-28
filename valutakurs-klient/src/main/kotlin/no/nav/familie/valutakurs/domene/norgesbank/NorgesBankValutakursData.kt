package no.nav.familie.valutakurs.domene.norgesbank

import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateAttributes
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateKey
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "GenericData")
data class NorgesBankValutakursData(
    @field:JacksonXmlProperty(localName = "DataSet")
    val dataSet: NorgesBankValutakursDataSet,
)

data class NorgesBankValutakursDataSet(
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Series")
    val series: NorgesBankValutakursSeries,
)

data class NorgesBankValutakursSeries(
    @field:JacksonXmlElementWrapper
    @field:JacksonXmlProperty(localName = "SeriesKey")
    val seriesKeys: List<SDMXExchangeRateKey>,
    @field:JacksonXmlElementWrapper
    @field:JacksonXmlProperty(localName = "Attributes")
    val attributes: List<SDMXExchangeRateAttributes>,
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Obs")
    val observations: SDMXExchangeRate,
)
