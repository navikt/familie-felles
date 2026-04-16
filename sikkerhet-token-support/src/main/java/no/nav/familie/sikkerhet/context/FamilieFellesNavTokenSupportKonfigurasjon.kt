package no.nav.familie.sikkerhet.context

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Konfigurerer [NavTokenSupportTokenContext] som [TokenContext]-implementasjon.
 *
 * Importer denne i applikasjonens konfigurasjonsklasse for å aktivere Nav token-support:
 *
 * ```kotlin
 * @Import(FamilieFellesNavTokenSupportKonfigurasjon::class)
 * @SpringBootApplication
 * class MyApp
 * ```
 */
@Configuration(proxyBeanMethods = false)
class FamilieFellesNavTokenSupportKonfigurasjon {
    @Bean
    fun familieFellesNavTokenSupportTokenContext(): NavTokenSupportTokenContext = NavTokenSupportTokenContext()
}
