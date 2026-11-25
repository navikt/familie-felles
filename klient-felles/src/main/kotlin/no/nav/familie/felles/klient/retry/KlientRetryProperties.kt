package no.nav.familie.felles.klient.retry

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.validator.constraints.time.DurationMax
import org.hibernate.validator.constraints.time.DurationMin
import java.time.Duration

/**
 * Retry-innstillinger for en klient. Hver klient binder sin egen instans, for eksempel under
 * `familie.felles.klient.pdl.retry`. Merk feltet med `@field:Valid` i klientens properties for å validere ved oppstart.
 *
 * [maxRetries] teller nye forsøk etter det første kallet. Ventetiden mellom forsøkene er konstant [delay],
 * med jitter på inntil [delay] / 2.
 */
class KlientRetryProperties(
    val enabled: Boolean = true,
    @field:Min(1)
    @field:Max(5)
    val maxRetries: Long = 2,
    @field:DurationMin(millis = 1)
    @field:DurationMax(millis = 1_000)
    val delay: Duration = Duration.ofMillis(100),
)
