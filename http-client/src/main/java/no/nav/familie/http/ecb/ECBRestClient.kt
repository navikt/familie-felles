package no.nav.familie.http.ecb

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.ecb.config.ECBRestClientConfig
import no.nav.familie.http.ecb.domene.ECBExchangeRatesData
import no.nav.familie.http.ecb.domene.ExchangeRate
import no.nav.familie.http.ecb.domene.toExchangeRates
import no.nav.familie.http.ecb.exception.ECBClientException
import no.nav.familie.http.ecb.exception.ECBTransformationException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
@Import(ECBRestClientConfig::class)
class ECBRestClient(@Qualifier("ecbRestTemplate") private val restOperations: RestOperations, @Value("\${ECB_API_URL}") private val ecbApiUrl: String = "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/") : AbstractRestClient(restOperations, "ecb") {

    /**
     * Henter valutakurser fra ECB (European Central Bank) for *currencies*
     * @param frequency spesifiserer om valutakurser skal hentes for spesifikk dag eller for måned.
     * @param currencies liste over valutakurser som skal hentes.
     * @param exchangeRateDate dato man ønsker valutakurser for. Dersom *frequency* er MONTHLY hentes forrige måneds kurs, med mindre man spør med *exchangeRateDate* = siste dag i mnd.
     * @return Liste over valutakurser med tilhørende kode, kurs og dato.
     */
    fun getExchangeRates(frequency: Frequency, currencies: List<String>, exchangeRateDate: LocalDate): List<ExchangeRate> {
        val uri = URI.create("${ecbApiUrl}${frequency.toFrequencyParam()}.${toCurrencyParams(currencies)}.EUR.SP00.A/${frequency.toQueryParams(exchangeRateDate)}")
        try {
            return getForEntity<ECBExchangeRatesData>(uri).toExchangeRates()
        } catch (e: RestClientResponseException) {
            throw ECBClientException("Kall mot European Central Bank feiler med statuskode ${e.rawStatusCode} for $currencies på dato: $exchangeRateDate", e)
        } catch (e: ECBTransformationException) {
            throw ECBClientException(e.message, e)
        } catch (e: NullPointerException) {
            throw ECBClientException("Fant ingen valutakurser for $currencies på dato: $exchangeRateDate ved kall mot European Central Bank", e)
        } catch (e: Exception) {
            throw ECBClientException("Ukjent feil ved kall mot European Central Bank", e)
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
