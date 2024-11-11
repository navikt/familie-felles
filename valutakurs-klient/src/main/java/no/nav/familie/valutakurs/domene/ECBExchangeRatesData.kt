package no.nav.familie.valutakurs.domene
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import no.nav.familie.valutakurs.exception.ValutakursTransformationException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import kotlin.jvm.Throws

@JacksonXmlRootElement(localName = "GenericData")
data class ECBExchangeRatesData(
    @field:JacksonXmlProperty(localName = "DataSet")
    val ecbExchangeRatesDataSet: ECBExchangeRatesDataSet,
)

fun ECBExchangeRatesData.exchangeRatesForCurrency(currency: String): List<ECBExchangeRate> =
    this.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies
        .filter {
            it.ecbExchangeRateKeys.any { ecbKeyValue ->
                ecbKeyValue.id == "CURRENCY" && ecbKeyValue.value == currency
            }
        }.flatMap { it.ecbExchangeRates }

@Throws(ValutakursTransformationException::class)
fun ECBExchangeRatesData.toExchangeRates(): List<ExchangeRate> {
    try {
        return this.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies
            .flatMap { ecbExchangeRatesForCurrency ->
                ecbExchangeRatesForCurrency.ecbExchangeRates
                    .map { ecbExchangeRate ->
                        val currency = ecbExchangeRatesForCurrency.ecbExchangeRateKeys.first { it.id == "CURRENCY" }.value
                        val frequency = ecbExchangeRatesForCurrency.ecbExchangeRateKeys.first { it.id == "FREQ" }.value
                        val date: LocalDate =
                            if (frequency == "D") {
                                LocalDate.parse(ecbExchangeRate.date.value)
                            } else {
                                YearMonth
                                    .parse(ecbExchangeRate.date.value)
                                    .atEndOfMonth()
                            }
                        ExchangeRate(currency, ecbExchangeRate.ecbExchangeRateValue.value, date)
                    }
            }
    } catch (e: NoSuchElementException) {
        throw ValutakursTransformationException("Feil ved transformering av ECB data. Respons mangler nødvendig informasjon.", e)
    } catch (e: DateTimeParseException) {
        throw ValutakursTransformationException("Feil ved transformering av ECB data. Respons inneholder ugyldig datoformat.", e)
    }
}
