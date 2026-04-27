package no.nav.familie.sikkerhet.context

import java.time.Instant

/**
 * Token-konteksten for innkommende HTTP-forespørsler, uavhengig av sikkerhetsrammeverk.
 *
 * Velg implementasjon ved å importere én konfigurasjon i appen:
 * - `@Import(FamilieFellesNavTokenSupportKonfigurasjon::class)` — Nav token-support
 * - `@Import(FamilieFellesSpringSecurityKonfigurasjon::class)` — Spring Security
 *
 * Spring feiler ved oppstart med [TokenContextKonfigurasjonException] hvis ingen eller flere
 * konfigurasjoner importeres. Se [TokenContextValidationAutoConfiguration].
 */
interface TokenContext {
    /**
     * Henter claimet som en streng for den angitte issueren.
     * Returnerer null hvis claimet ikke finnes.
     */
    fun getClaimAsString(
        claim: String,
        issuer: String = "azuread",
    ): String?

    /**
     * Henter claimet som en liste av strenger for den angitte issueren.
     * Returnerer null hvis claimet ikke finnes.
     */
    fun getClaimAsStringList(
        claim: String,
        issuer: String = "azuread",
    ): List<String>?

    /** Returnerer true hvis det finnes et validert token for den angitte issueren. */
    fun hasTokenFor(issuer: String): Boolean

    /** Henter bearer token for den angitte issueren, eller null hvis det ikke finnes. */
    fun getBearerToken(issuer: String = "azuread"): String?

    /** Alle issuere det finnes validerte tokens for. */
    fun issuers(): Collection<String>

    /** Utløpstidspunktet for tokenet til den angitte issueren, eller null hvis det ikke finnes. */
    fun getExpiry(issuer: String = "azuread"): Instant?
}
