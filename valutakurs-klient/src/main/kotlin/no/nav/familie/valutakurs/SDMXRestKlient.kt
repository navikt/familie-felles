package no.nav.familie.valutakurs

import no.nav.familie.valutakurs.exception.IngenValutakursException
import no.nav.familie.valutakurs.exception.ValutakursException
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.URI

abstract class SDMXRestKlient(
    @PublishedApi
    internal val restClient: RestClient,
) {
    inline fun <reified T : Any> hentValutakurs(url: URI): T {
        try {
            return restClient
                .get()
                .uri(url)
                .accept(MediaType.parseMediaType(APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA))
                .retrieve()
                .body(T::class.java)
                ?: throw IngenValutakursException("Fant ingen valutakurs for url $url", null)
        } catch (e: RestClientResponseException) {
            throw ValutakursException(
                "Henting av valutakurs feiler med statuskode ${e.statusCode.value()}.",
                e,
            )
        } catch (e: IngenValutakursException) {
            throw e
        } catch (e: Exception) {
            throw ValutakursException("Ukjent feil ved ved henting av valutakurs", e)
        }
    }

    companion object {
        const val APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA = "application/vnd.sdmx.genericdata+xml;version=2.1"
    }
}
