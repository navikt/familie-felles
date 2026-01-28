package no.nav.familie.valutakurs.domene.sdmx

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class SDMXExchangeRateAttributes(
    @field:JacksonXmlProperty(isAttribute = true)
    val id: String,
    @field:JacksonXmlProperty(isAttribute = true)
    val value: String,
)
