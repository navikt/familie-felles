package no.nav.familie.http.ecb.domene
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import no.nav.familie.http.ecb.exception.ECBTransformationException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import kotlin.jvm.Throws

@JacksonXmlRootElement(localName = "GenericData")
data class ECBExchangeRatesData(
    @field:JacksonXmlProperty(localName = "DataSet")
    val ecbExchangeRatesDataSet: ECBExchangeRatesDataSet
)

fun ECBExchangeRatesData.exchangeRatesForCurrency(currency: String): List<ECBExchangeRate> {
    return this.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies.filter {
        it.ecbExchangeRateKeys.any {
                ecbKeyValue ->
            ecbKeyValue.id == "CURRENCY" && ecbKeyValue.value == currency
        }
    }.flatMap { it.ecbExchangeRates }
}

@Throws(ECBTransformationException::class)
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
                                YearMonth.parse(ecbExchangeRate.date.value)
                                    .atEndOfMonth()
                            }
                        ExchangeRate(currency, ecbExchangeRate.ecbExchangeRateValue.value, date)
                    }
            }
    } catch (e: NoSuchElementException) {
        throw ECBTransformationException("Feil ved transformering av ECB data. Respons mangler nødvendig informasjon.", e)
    } catch (e: DateTimeParseException) {
        throw ECBTransformationException("Feil ved transformering av ECB data. Respons inneholder ugyldig datoformat.", e)
    }
}
