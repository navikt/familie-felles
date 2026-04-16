package no.nav.familie.sikkerhet.context

import java.time.Instant

/**
 * Statisk holder for [TokenContext]-strategien, tilsvarende Spring Securitys `SecurityContextHolder`.
 *
 * [TokenContextValidationAutoConfiguration] sørger for at nøyaktig én [TokenContext] er konfigurert
 * ved oppstart, så context aldri er null i en kjørende applikasjon.
 *
 * [setContext] og [clearContext] er `internal` og kun beregnet for tester i dette biblioteket.
 */
object TokenContextHolder {
    @Volatile
    private var context: TokenContext? = null

    internal fun setContext(tokenContext: TokenContext) {
        context = tokenContext
    }

    internal fun clearContext() {
        context = null
    }

    internal fun getContext(): TokenContext =
        context
            ?: throw TokenContextConfigurationException(
                "Ingen TokenContext er konfigurert. " +
                    "Importer én av følgende konfigurasjoner med @Import:\n" +
                    "  FamilieFellesNavTokenSupportKonfigurasjon  (fra sikkerhet-token-support)\n" +
                    "  FamilieFellesSpringSecurityKonfigurasjon   (fra sikkerhet-spring-security)",
            )

    fun getClaimAsString(
        claim: String,
        issuer: String = "azuread",
    ): String? = getContext().getClaimAsString(claim, issuer)

    fun getClaimAsStringList(
        claim: String,
        issuer: String = "azuread",
    ): List<String>? = getContext().getClaimAsStringList(claim, issuer)

    fun hasTokenFor(issuer: String): Boolean = getContext().hasTokenFor(issuer)

    fun getBearerToken(issuer: String): String? = getContext().getBearerToken(issuer)

    fun issuers(): Collection<String> = getContext().issuers()

    fun getExpiry(issuer: String = "azuread"): Instant? = getContext().getExpiry(issuer)
}
