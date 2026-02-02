package no.nav.familie.restklient.client

import org.springframework.web.client.RestOperations
import java.net.URI

/**
 * Utvidelse av AbstractRestClient for tjenester som implementerer ping.
 */
abstract class AbstractPingableRestClient(
    operations: RestOperations,
    metricsPrefix: String,
) : AbstractRestClient(operations, metricsPrefix),
    Pingable {
    abstract val pingUri: URI

    override fun ping() {
        try {
            super.getForEntity<String>(pingUri, null)
        } catch (e: ResponseBodyNullException) {
            // Ignorer, ping kan ha null-body
        }
    }

    override fun toString(): String = this::class.simpleName + " [operations=" + operations + "]"
}
