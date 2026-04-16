package no.nav.familie.sikkerhet.context

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

/**
 * [TokenContext]-implementasjon som bruker Spring Securitys [SecurityContextHolder].
 *
 * @param issuerNameMapping Valgfri mapping fra issuer-URL (f.eks. verdien i `iss`-claimet) til
 *   kortnavnet som brukes i biblioteket (f.eks. `"azuread"`, `"tokenx"`).
 *   Hvis issuer-URLen finnes i mappingen brukes kortnavnet; ellers brukes issuer-URLen direkte.
 */
class SpringSecurityTokenContext(
    private val issuerNameMapping: Map<String, String> = emptyMap(),
) : TokenContext {
    override fun getClaimAsString(
        claim: String,
        issuer: String,
    ): String? = currentJwt(issuer)?.claims?.get(claim)?.toString()

    override fun getClaimAsStringList(
        claim: String,
        issuer: String,
    ): List<String>? {
        val claimValue = currentJwt(issuer)?.claims?.get(claim) ?: return null
        return when (claimValue) {
            is List<*> -> claimValue.filterIsInstance<String>()
            is String -> listOf(claimValue)
            else -> null
        }
    }

    override fun hasTokenFor(issuer: String): Boolean = currentJwt(issuer) != null

    override fun getBearerToken(issuer: String): String? = currentJwt(issuer)?.tokenValue

    override fun issuers(): Collection<String> {
        val jwt = currentJwtFromContext() ?: return emptyList()
        val issUrl = jwt.issuer?.toString() ?: return emptyList()
        return listOf(issUrl)
    }

    override fun getExpiry(issuer: String): Instant? = currentJwt(issuer)?.expiresAt

    private fun currentJwt(issuer: String): Jwt? {
        val jwt = currentJwtFromContext() ?: return null
        val issUrl = jwt.issuer?.toString() ?: return null
        return jwt.takeIf { issUrl == issuer || issuerNameMapping[issUrl] == issuer }
    }

    private fun currentJwtFromContext(): Jwt? = (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)?.token
}
