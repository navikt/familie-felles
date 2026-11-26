package no.nav.familie.oppgave.config

import jakarta.validation.constraints.Pattern
import no.nav.familie.felles.klient.KlientMiljø
import no.nav.familie.felles.klient.KlientMiljø.PREPROD
import no.nav.familie.felles.klient.KlientMiljø.PROD
import org.hibernate.validator.constraints.URL
import org.hibernate.validator.constraints.time.DurationMax
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("familie.felles.klient.oppgave")
class OppgaveKlientProperties(
    @field:Pattern(regexp = ".*\\S.*")
    @field:URL(regexp = "^https?://.*")
    val url: String? = null,
    @field:Pattern(regexp = "api://[a-z0-9-]+\\.[a-z0-9-]+\\.[a-z0-9-]+/\\.default")
    val scope: String? = null,
    @field:DurationMin(millis = 1)
    @field:DurationMax(millis = Int.MAX_VALUE.toLong())
    val connectTimeout: Duration = Duration.ofSeconds(2),
    @field:DurationMin(millis = 1)
    @field:DurationMax(millis = Int.MAX_VALUE.toLong())
    val readTimeout: Duration = Duration.ofSeconds(10),
) {
    internal fun hentUrl(miljø: KlientMiljø?): String =
        url
            ?: when (miljø) {
                PREPROD -> "https://oppgave.dev-fss-pub.nais.io"

                PROD -> "https://oppgave.prod-fss-pub.nais.io"

                null -> throw IllegalStateException(
                    "Konfigurer familie.felles.klient.miljo eller familie.felles.klient.oppgave.url",
                )
            }

    internal fun hentScope(miljø: KlientMiljø?): String =
        scope
            ?: when (miljø) {
                PREPROD -> "api://dev-fss.oppgavehandtering.oppgave/.default"

                PROD -> "api://prod-fss.oppgavehandtering.oppgave/.default"

                null -> throw IllegalStateException(
                    "Konfigurer familie.felles.klient.miljo eller familie.felles.klient.oppgave.scope",
                )
            }
}
