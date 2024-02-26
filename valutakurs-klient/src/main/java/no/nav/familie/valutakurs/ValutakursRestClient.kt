package no.nav.familie.valutakurs

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.valutakurs.config.ValutakursRestClientConfig
import no.nav.familie.valutakurs.domene.ECBExchangeRatesData
import no.nav.familie.valutakurs.domene.ExchangeRate
import no.nav.familie.valutakurs.domene.toExchangeRates
import no.nav.familie.valutakurs.exception.ValutakursClientException
import no.nav.familie.valutakurs.exception.ValutakursTransformationException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate

@Component
@Import(ValutakursRestClientConfig::class)
class ValutakursRestClient(
    @Qualifier("ecbRestTemplate") private val restOperations: RestTemplate,
    @Value("\${ECB_API_URL}") private val ecbApiUrl: String = "https://data-api.ecb.europa.eu/service/data/EXR/",
) : AbstractRestClient(RestClient.create(restOperations), "ecb") {
    /**
     * Henter valutakurser fra ECB (European Central Bank) for *currencies*
     * @param frequency spesifiserer om valutakurser skal hentes for spesifikk dag eller for måned.
     * @param currencies liste over valutakurser som skal hentes.
     * @param exchangeRateDate dato man ønsker valutakurser for. Dersom *frequency* er MONTHLY hentes forrige måneds kurs, med mindre man spør med *exchangeRateDate* = siste dag i mnd.
     * @return Liste over valutakurser med tilhørende kode, kurs og dato.
     */
    fun hentValutakurs(
        frequency: Frequency,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
    ): List<ExchangeRate> {
        val uri =
            URI.create(
                "${ecbApiUrl}${frequency.toFrequencyParam()}.${toCurrencyParams(
                    currencies,
                )}.EUR.SP00.A/${frequency.toQueryParams(exchangeRateDate)}",
            )
        try {
            HttpHeaders().apply {
                add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
            }
            return getForEntity<ECBExchangeRatesData>(uri).toExchangeRates()
        } catch (e: RestClientResponseException) {
            throw ValutakursClientException(
                "Kall mot European Central Bank feiler med statuskode ${e.statusCode.value()} for $currencies på dato: $exchangeRateDate",
                e,
            )
        } catch (e: ValutakursTransformationException) {
            throw ValutakursClientException(e.message, e)
        } catch (e: NullPointerException) {
            throw ValutakursClientException(
                "Fant ingen valutakurser for $currencies på dato: $exchangeRateDate ved kall mot European Central Bank",
                e,
            )
        } catch (e: Exception) {
            throw ValutakursClientException("Ukjent feil ved kall mot European Central Bank", e)
        }
    }

    private fun toCurrencyParams(currencies: List<String>): String {
        return currencies.reduceIndexed { index, params, currency -> if (index != 0) "$params+$currency" else currency }
    }
}

enum class Frequency {
    Daily,
    Monthly,
    ;

    fun toFrequencyParam() =
        when (this) {
            Daily -> "D"
            Monthly -> "M"
        }

    fun toQueryParams(exchangeRateDate: LocalDate) =
        when (this) {
            Daily -> "?startPeriod=$exchangeRateDate&endPeriod=$exchangeRateDate"
            Monthly -> "?endPeriod=$exchangeRateDate&lastNObservations=1"
        }
}
