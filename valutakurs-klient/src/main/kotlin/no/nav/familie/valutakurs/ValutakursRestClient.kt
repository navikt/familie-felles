package no.nav.familie.valutakurs

import no.nav.familie.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.familie.valutakurs.domene.Valutakurs
import no.nav.familie.valutakurs.domene.ecb.ECBValutakursData
import no.nav.familie.valutakurs.domene.ecb.Frequency
import no.nav.familie.valutakurs.domene.ecb.toExchangeRates
import no.nav.familie.valutakurs.exception.IngenValutakursException
import no.nav.familie.valutakurs.exception.ValutakursException
import no.nav.familie.valutakurs.exception.ValutakursTransformationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
@Import(SDMXValutakursRestKlientConfig::class)
class ValutakursRestClient(
    @Qualifier("sdmxXmlRestTemplate") restOperations: RestOperations,
    @param:Value("\${ECB_API_URL}") private val ecbApiUrl: String = "https://data-api.ecb.europa.eu/service/data/EXR/",
) : SDMXRestKlient(restOperations, "ecb") {
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
    ): List<Valutakurs> {
        val uri = lagECBURI(frequency, currencies, exchangeRateDate)
        try {
            logger.info("Henter valutakurs fra ECB for ${currencies.toCurrencyParams()} på dato $exchangeRateDate")
            return hentValutakurs<ECBValutakursData>(uri).toExchangeRates()
        } catch (e: ValutakursTransformationException) {
            throw ValutakursException(e.message, e)
        } catch (e: NullPointerException) {
            throw IngenValutakursException(
                "Fant ingen valutakurser.",
                e,
            )
        }
    }

    private fun lagECBURI(
        frequency: Frequency,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
    ): URI =
        URI.create(
            "${ecbApiUrl}${frequency.toFrequencyParam()}.${currencies.toCurrencyParams()}.EUR.SP00.A${frequency.toQueryParams(
                exchangeRateDate,
            )}",
        )

    private fun List<String>.toCurrencyParams(): String =
        this.reduceIndexed { index, params, currency -> if (index != 0) "$params+$currency" else currency }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ValutakursRestClient::class.java)
    }
}
