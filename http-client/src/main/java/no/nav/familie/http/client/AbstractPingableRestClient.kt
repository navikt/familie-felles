package no.nav.familie.http.client

import org.springframework.web.client.RestClient
import java.net.URI

/**
 * Utvidelse av AbstractRestClient for tjenester som implementerer ping.
 */
abstract class AbstractPingableRestClient(
    restClient: RestClient,
    metricsPrefix: String,
) : AbstractRestClient(restClient, metricsPrefix), Pingable {
    abstract val pingUri: URI

    override fun ping() {
        super.getForEntity<String>(pingUri, null)
    }

    override fun toString(): String = this::class.simpleName + " [restClient=" + restClient + "]"
}
