package no.nav.familie.sikkerhet.context

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Konfigurerer [SpringSecurityTokenContext] som [TokenContext]-implementasjon.
 *
 * Importer denne i applikasjonens konfigurasjonsklasse for å aktivere Spring Security:
 *
 * ```kotlin
 * @Import(FamilieFellesSpringSecurityKonfigurasjon::class)
 * @SpringBootApplication
 * class MyApp
 * ```
 *
 * [SpringSecurityTokenContext.issuerNameMapping] bygges automatisk fra miljøvariablene
 * `AZURE_OPENID_CONFIG_ISSUER` → `"azuread"`, `TOKEN_X_ISSUER` → `"tokenx"` og `SELVBETJENING_ISSUER` → `"selvbetjening"`.
 */
@Configuration(proxyBeanMethods = false)
class FamilieFellesSpringSecurityKonfigurasjon(
    @Value("\${AZURE_OPENID_CONFIG_ISSUER:}") private val azureIssuer: String,
    @Value("\${TOKEN_X_ISSUER:}") private val tokenxIssuer: String,
    @Value("\${IDPORTEN_ISSUER:}") private val selvbetjeningIssuer: String,
) {
    @Bean
    fun familieFellesSpringSecurityTokenContext(): SpringSecurityTokenContext {
        val mapping =
            buildMap {
                if (azureIssuer.isNotEmpty()) put(azureIssuer, "azuread")
                if (tokenxIssuer.isNotEmpty()) put(tokenxIssuer, "tokenx")
                if (selvbetjeningIssuer.isNotEmpty()) put(selvbetjeningIssuer, "selvbetjening")
            }
        return SpringSecurityTokenContext(issuerNameMapping = mapping)
    }
}
