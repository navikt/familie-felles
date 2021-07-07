package no.nav.familie.webflux.health

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.webflux.client.Pingable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.core.NestedExceptionUtils

/**
 * Helseindikator for pingbare tjenester.
 */
abstract class AbstractHealthIndicator(private val pingable: Pingable,
                                       metricsNavn: String,
                                       private val statusCode: String = "DOWN-NONCRITICAL") : HealthIndicator {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val failureCounter: Counter = Metrics.counter(metricsNavn, "status", "nede")

    override fun health(): Health {
        return try {
            pingable.ping()
            Health.up().build()
        } catch (e: Exception) {
            failureCounter.increment()
            log.info("Feil ved helsesjekk ${this::class.simpleName}", e)
            Health.status(statusCode)
                    .withDetail("Feilmelding", NestedExceptionUtils.getMostSpecificCause(e).javaClass.name + ": " + e.message)
                    .build()
        }
    }

}
