package no.nav.familie.sikkerhet.context

import no.nav.familie.sikkerhet.context.TokenContextHolder.clearContext
import no.nav.familie.sikkerhet.context.TokenContextHolder.getContext
import no.nav.familie.sikkerhet.context.TokenContextHolder.setContext
import java.time.Instant

/**
 * Statisk holder for [TokenContext]-strategien, tilsvarende Spring Securitys `SecurityContextHolder`.
 *
 * [TokenContextValidationAutoConfiguration] sørger for at nøyaktig én [TokenContext] er konfigurert
 * ved oppstart, så context aldri er null i en kjørende applikasjon.
 *
 * [setContext] og [clearContext] er `internal` for å forhindre at konteksten settes eller nullstilles i produksjonskode.
 */
object TokenContextHolder {
    @Volatile
    private var context: TokenContext? = null

    /**
     * Registrerer [TokenContext]-implementasjonen som skal brukes.
     *
     * Kalles av [TokenContextValidationAutoConfiguration] ved oppstart.
     * Tilgjengelig for tester via `TokenContextTestHelper` i test-jar.
     */
    internal fun setContext(tokenContext: TokenContext) {
        context = tokenContext
    }

    /**
     * Nullstiller konteksten slik at [getContext] igjen kaster [TokenContextKonfigurasjonException].
     *
     * Brukes i tester for å simulere fravær av konfigurasjon og for opprydding etter tester som setter kontekst.
     * Tilgjengelig for tester via `TokenContextTestHelper` i test-jar.
     */
    internal fun clearContext() {
        context = null
    }

    /**
     * Returnerer den registrerte [TokenContext]-implementasjonen.
     *
     * @throws TokenContextKonfigurasjonException hvis ingen kontekst er satt.
     */
    internal fun getContext(): TokenContext =
        context
            ?: throw TokenContextKonfigurasjonException(
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
