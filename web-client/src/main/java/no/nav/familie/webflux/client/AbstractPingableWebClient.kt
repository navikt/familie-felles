package no.nav.familie.webflux.client

import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

/**
 * Utvidelse av AbstractWebClient for tjenester som implementerer ping.
 */
abstract class AbstractPingableWebClient(
    webClient: WebClient,
    metricsPrefix: String
) : AbstractWebClient(webClient, metricsPrefix), Pingable {

    abstract val pingUri: URI

    override fun ping() {
        super.getForEntity<String>(pingUri, null)
    }

    override fun toString(): String = this::class.simpleName + " [operations=" + webClient + "]"
}
