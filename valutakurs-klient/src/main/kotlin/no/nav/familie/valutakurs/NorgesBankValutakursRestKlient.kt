package no.nav.familie.valutakurs

import no.nav.familie.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.familie.valutakurs.domene.Valutakurs
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursData
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursMapper.tilValutakurs
import no.nav.familie.valutakurs.exception.IngenValutakursException
import no.nav.familie.valutakurs.exception.NorgesBankValutakursMappingException
import no.nav.familie.valutakurs.exception.ValutakursException
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
class NorgesBankValutakursRestKlient(
    @Qualifier("sdmxXmlRestTemplate") restOperations: RestOperations,
    @param:Value("\${NORGES_BANK_API_URL}") private val norgesBankApiUrl: String = "https://data.norges-bank.no/api/data/EXR/",
) : SDMXRestKlient(restOperations, "norges-bank") {
    /**
     * Henter valutakurser fra ECB (European Central Bank) via Norges Bank for *valuta*
     * @param frekvens spesifiserer om valutakurs skal hentes for virkedag, måned eller år.
     * @param valuta valuta kurs skal hentes for.
     * @param kursDato dato man ønsker valutakurs for.
     * @return Valutakurs med tilhørende kode, kurs og dato.
     */
    fun hentValutakurs(
        frekvens: Frekvens,
        valuta: String,
        kursDato: LocalDate,
    ): Valutakurs {
        val uri = lagNorgesBankURI(frekvens, valuta, kursDato)
        try {
            logger.info("Henter valutakurs fra Norges Bank for $valuta på dato $kursDato")
            return hentValutakurs<NorgesBankValutakursData>(uri)
                .tilValutakurs(valuta = valuta, frekvens = frekvens, kursDato = kursDato)
        } catch (e: NorgesBankValutakursMappingException) {
            throw ValutakursException(message = e.message!!, e)
        } catch (e: NullPointerException) {
            throw IngenValutakursException("Fant ingen valutakurser.", e)
        }
    }

    private fun lagNorgesBankURI(
        frekvens: Frekvens,
        valuta: String,
        kursDato: LocalDate,
    ): URI =
        URI.create(
            "${norgesBankApiUrl}${frekvens.verdi}.$valuta.NOK.SP?format=sdmx-generic-2.1&startPeriod=$kursDato&endPeriod=$kursDato",
        )

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NorgesBankValutakursRestKlient::class.java)
    }
}
