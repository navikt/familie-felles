package no.nav.familie.webflux.client

import no.nav.familie.http.client.Pingable
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

/**
 * Utvidelse av AbstractRestClient for tjenester som implementerer ping.
 */
abstract class AbstractPingableRestClient(webClient: WebClient,
                                          metricsPrefix: String) : AbstractWebClient(webClient, metricsPrefix), Pingable {

    abstract val pingUri: URI

    override fun ping() {
        super.getForEntity<String>(pingUri)
    }

    override fun toString(): String = this::class.simpleName + " [operations=" + webClient + "]"
}
