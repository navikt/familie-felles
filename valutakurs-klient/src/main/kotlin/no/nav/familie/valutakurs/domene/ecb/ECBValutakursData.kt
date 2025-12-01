package no.nav.familie.valutakurs.domene.ecb

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import no.nav.familie.valutakurs.domene.Valutakurs
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesDataSet
import no.nav.familie.valutakurs.exception.ValutakursTransformationException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@JacksonXmlRootElement(localName = "GenericData")
data class ECBValutakursData(
    @field:JacksonXmlProperty(localName = "DataSet")
    val sdmxExchangeRatesDataSet: SDMXExchangeRatesDataSet,
)

fun ECBValutakursData.exchangeRatesForCurrency(currency: String): List<SDMXExchangeRate> =
    this.sdmxExchangeRatesDataSet.sdmxExchangeRatesForCurrencies
        .filter {
            it.sdmxExchangeRateKeys.any { ecbKeyValue ->
                ecbKeyValue.id == "CURRENCY" && ecbKeyValue.value == currency
            }
        }.flatMap { it.sdmxExchangeRates }

@Throws(ValutakursTransformationException::class)
fun ECBValutakursData.toExchangeRates(): List<Valutakurs> {
    try {
        return this.sdmxExchangeRatesDataSet.sdmxExchangeRatesForCurrencies
            .flatMap { ecbExchangeRatesForCurrency ->
                ecbExchangeRatesForCurrency.sdmxExchangeRates
                    .map { ecbExchangeRate ->
                        val currency = ecbExchangeRatesForCurrency.sdmxExchangeRateKeys.first { it.id == "CURRENCY" }.value
                        val frequency = ecbExchangeRatesForCurrency.sdmxExchangeRateKeys.first { it.id == "FREQ" }.value
                        val date: LocalDate =
                            if (frequency == "D") {
                                LocalDate.parse(ecbExchangeRate.date.value)
                            } else {
                                YearMonth
                                    .parse(ecbExchangeRate.date.value)
                                    .atEndOfMonth()
                            }
                        Valutakurs(currency, ecbExchangeRate.sdmxExchangeRateValue.value, date)
                    }
            }
    } catch (e: NoSuchElementException) {
        throw ValutakursTransformationException("Feil ved transformering av ECB data. Respons mangler nødvendig informasjon.", e)
    } catch (e: DateTimeParseException) {
        throw ValutakursTransformationException("Feil ved transformering av ECB data. Respons inneholder ugyldig datoformat.", e)
    }
}
