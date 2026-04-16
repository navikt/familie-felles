package no.nav.familie.sikkerhet.context

import org.springframework.boot.autoconfigure.AutoConfiguration

/**
 * Fail-fast auto-konfigurasjon som validerer at nøyaktig én [TokenContext]-bean er registrert
 * ved oppstart. Kaster [TokenContextConfigurationException] hvis ingen eller mer enn én bean finnes.
 *
 * Aktiveres automatisk av Spring Boot. Importer nøyaktig én av:
 * - [FamilieFellesNavTokenSupportKonfigurasjon] (fra `sikkerhet-token-support`)
 * - [FamilieFellesSpringSecurityKonfigurasjon] (fra `sikkerhet-spring-security`)
 */
@AutoConfiguration
class TokenContextValidationAutoConfiguration(
    tokenContexts: List<TokenContext>,
) {
    init {
        val feilretting =
            "Importer én av følgende konfigurasjoner med @Import:\n" +
                "  FamilieFellesNavTokenSupportKonfigurasjon  (fra sikkerhet-token-support)\n" +
                "  FamilieFellesSpringSecurityKonfigurasjon   (fra sikkerhet-spring-security)"
        if (tokenContexts.isEmpty()) {
            throw TokenContextConfigurationException("Ingen TokenContext er konfigurert. $feilretting")
        }
        if (tokenContexts.size > 1) {
            throw TokenContextConfigurationException("Flere TokenContext'er er konfigurert. $feilretting")
        }
        TokenContextHolder.setContext(tokenContexts.single())
    }
}
