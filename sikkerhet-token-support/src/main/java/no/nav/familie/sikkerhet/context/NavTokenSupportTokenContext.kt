package no.nav.familie.sikkerhet.context

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import java.time.Instant

/**
 * [TokenContext]-implementasjon som bruker Nav token-support sin [SpringTokenValidationContextHolder].
 */
class NavTokenSupportTokenContext : TokenContext {
    private val holder = SpringTokenValidationContextHolder()

    override fun getClaimAsString(
        claim: String,
        issuer: String,
    ): String? = getValidationContext()?.getClaims(issuer)?.get(claim)?.toString()

    override fun getClaimAsStringList(
        claim: String,
        issuer: String,
    ): List<String>? {
        val claimValue = getValidationContext()?.getClaims(issuer)?.get(claim) ?: return null
        return when (claimValue) {
            is List<*> -> claimValue.filterIsInstance<String>()
            is String -> listOf(claimValue)
            else -> null
        }
    }

    override fun hasTokenFor(issuer: String): Boolean = getValidationContext()?.hasTokenFor(issuer) ?: false

    override fun getBearerToken(issuer: String): String? = getValidationContext()?.getJwtToken(issuer)?.encodedToken

    override fun issuers(): Collection<String> = getValidationContext()?.issuers.orEmpty()

    override fun getExpiry(issuer: String): Instant? = getValidationContext()?.getClaims(issuer)?.expirationTime?.toInstant()

    private fun getValidationContext() = runCatching { holder.getTokenValidationContext() }.getOrNull()
}
