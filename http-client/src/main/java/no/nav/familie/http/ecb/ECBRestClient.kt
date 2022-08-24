package no.nav.familie.http.ecb

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.config.ECBRestTemplate
import no.nav.familie.http.ecb.domene.ECBExchangeRatesData
import no.nav.familie.http.ecb.domene.ExchangeRate
import no.nav.familie.http.ecb.domene.toExchangeRates
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
@Import(ECBRestTemplate::class)
class ECBRestClient(@Qualifier("ecbRestTemplate") private val restOperations: RestOperations) : AbstractRestClient(restOperations, "ecb") {

    private final val ECBApiUrl = "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/"

    /**
     * Henter valutakurser fra ECB (European Central Bank) for *currencies*
     * @param frequency spesifiserer om valutakurser skal hentes for spesifikk dag eller for måned.
     * @param currencies liste over valutakurser som skal hentes.
     * @param exchangeRateDate dato man ønsker valutakurser for. Dersom *frequency* er MONTHLY hentes forrige måneds kurs, med mindre man spør med *exchangeRateDate* = siste dag i mnd.
     * @return Liste over valutakurser med tilhørende kode, kurs og dato.
     */
    fun getExchangeRates(frequency: Frequency, currencies: List<String>, exchangeRateDate: LocalDate): List<ExchangeRate> {
        val uri = URI.create("${ECBApiUrl}${frequency.toFrequencyParam()}.${toCurrencyParams(currencies)}.EUR.SP00.A/${frequency.toQueryParams(exchangeRateDate)}")
        try {
            return getForEntity<ECBExchangeRatesData>(uri).toExchangeRates()
        } catch (e: Exception) {
            throw ECBClientException("Fant ingen valutakurser for $currencies på dato: $exchangeRateDate fra European Central Bank", e)
        }
    }

    private fun toCurrencyParams(currencies: List<String>): String {
        return currencies.reduceIndexed { index, params, currency -> if (index != 0) "$params+$currency" else currency }
    }
}

enum class Frequency {
    Daily,
    Monthly;

    fun toFrequencyParam() = when (this) {
        Daily -> "D"
        Monthly -> "M"
    }
    fun toQueryParams(exchangeRateDate: LocalDate) = when (this) {
        Daily -> "?startPeriod=$exchangeRateDate&endPeriod=$exchangeRateDate"
        Monthly -> "?endPeriod=$exchangeRateDate&lastNObservations=1"
    }
}
