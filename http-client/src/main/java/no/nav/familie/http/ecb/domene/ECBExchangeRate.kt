package no.nav.familie.http.ecb.domene
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.math.BigDecimal

data class ECBExchangeRate(
    @field:JacksonXmlProperty(localName = "ObsDimension")
    val date: ECBExchangeRateDate,

    @field:JacksonXmlProperty(localName = "ObsValue")
    val ecbExchangeRateValue: ECBExchangeRateValue
)

data class ECBExchangeRateDate(
    @field:JacksonXmlProperty(isAttribute = true)
    val value: String
)

data class ECBExchangeRateValue(
    @field:JacksonXmlProperty(isAttribute = true)
    val value: BigDecimal
)
