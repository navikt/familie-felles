package no.nav.familie.valutakurs

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.valutakurs.exception.ValutakursException
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import java.net.URI

abstract class SDMXRestKlient(
    restOperations: RestOperations,
    metricsPrefix: String,
) : AbstractRestClient(restOperations, metricsPrefix) {
    inline fun <reified T : Any> hentValutakurs(url: URI): T? {
        try {
            return getForEntity(url, headers())
        } catch (e: RestClientResponseException) {
            throw ValutakursException(
                "Henting av valutakurs feiler med statuskode ${e.statusCode.value()}.",
                e,
            )
        } catch (e: Exception) {
            throw ValutakursException("Ukjent feil ved ved henting av valutakurs", e)
        }
    }

    fun headers(): HttpHeaders =
        HttpHeaders().apply {
            add(HttpHeaders.ACCEPT, APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA)
        }

    companion object {
        const val APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA = "application/vnd.sdmx.genericdata+xml;version=2.1"
    }
}
