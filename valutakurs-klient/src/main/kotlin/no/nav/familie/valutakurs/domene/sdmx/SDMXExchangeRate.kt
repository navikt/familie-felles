package no.nav.familie.valutakurs.domene.sdmx
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.math.BigDecimal

data class SDMXExchangeRate(
    @field:JacksonXmlProperty(localName = "ObsDimension")
    val date: SDMXExchangeRateDate,
    @field:JacksonXmlProperty(localName = "ObsValue")
    val sdmxExchangeRateValue: SDMXExchangeRateValue,
)

data class SDMXExchangeRateDate(
    @field:JacksonXmlProperty(isAttribute = true)
    val value: String,
)

data class SDMXExchangeRateValue(
    @field:JacksonXmlProperty(isAttribute = true)
    val value: BigDecimal,
)
