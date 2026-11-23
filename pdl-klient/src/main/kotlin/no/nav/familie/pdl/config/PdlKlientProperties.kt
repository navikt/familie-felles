package no.nav.familie.pdl.config

import jakarta.validation.constraints.Pattern
import no.nav.familie.felles.klient.KlientMiljo
import no.nav.familie.felles.klient.KlientMiljo.PREPROD
import no.nav.familie.felles.klient.KlientMiljo.PROD
import org.hibernate.validator.constraints.URL
import org.hibernate.validator.constraints.time.DurationMax
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("familie.felles.klient.pdl")
class PdlKlientProperties(
    @field:Pattern(regexp = ".*\\S.*")
    @field:URL(protocol = "https")
    val url: String? = null,
    @field:Pattern(regexp = "api://[a-z0-9-]+\\.[a-z0-9-]+\\.[a-z0-9-]+/\\.default")
    val scope: String? = null,
    @field:Pattern(regexp = ".*\\S.*")
    val behandlingsnummer: String? = null,
    @field:DurationMin(millis = 1)
    @field:DurationMax(millis = Int.MAX_VALUE.toLong())
    val connectTimeout: Duration = Duration.ofSeconds(2),
    @field:DurationMin(millis = 1)
    @field:DurationMax(millis = Int.MAX_VALUE.toLong())
    val readTimeout: Duration = Duration.ofSeconds(10),
) {
    internal fun hentUrl(miljo: KlientMiljo?): String =
        url
            ?: when (miljo) {
                PREPROD -> "https://pdl-api.dev-fss-pub.nais.io"

                PROD -> "https://pdl-api.prod-fss-pub.nais.io"

                null -> throw IllegalStateException(
                    "Konfigurer familie.felles.klient.miljo eller familie.felles.klient.pdl.url",
                )
            }

    internal fun hentScope(miljo: KlientMiljo?): String =
        scope
            ?: when (miljo) {
                PREPROD -> "api://dev-fss.pdl.pdl-api/.default"

                PROD -> "api://prod-fss.pdl.pdl-api/.default"

                null -> throw IllegalStateException(
                    "Konfigurer familie.felles.klient.miljo eller familie.felles.klient.pdl.scope",
                )
            }
}
