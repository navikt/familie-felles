package no.nav.familie.http.health

import io.micrometer.core.instrument.Counter
import no.nav.familie.http.client.Pingable
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.core.NestedExceptionUtils

/**
 * Helseindikator for pingbare tjenester.
 */
internal abstract class AbstractHealthIndicator(private val pingable: Pingable,
                                                private val statusCode: String = "DOWN-NONCRITICAL") : HealthIndicator {

    protected abstract val failureCounter: Counter

    override fun health(): Health {
        return try {
            pingable.ping()
            Health.up().build()
        } catch (e: Exception) {
            failureCounter.increment()
            Health.status(statusCode)
                    .withDetail("Feilmelding", NestedExceptionUtils.getMostSpecificCause(e).javaClass.name + ": " + e.message)
                    .build()
        }
    }

}
