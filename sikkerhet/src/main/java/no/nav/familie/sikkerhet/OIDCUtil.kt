package no.nav.familie.sikkerhet

import no.nav.familie.sikkerhet.context.TokenContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.util.Date

/**
 * Spring-bean med hjelpemetoder for å lese claims fra azuread-tokenet til innlogget Nav-ansatt.
 *
 * Kaster [JwtTokenInvalidException] hvis et forventet claim mangler i tokenet.
 * I `dev`- og `mock-auth`-profil returneres hardkodede testverdier.
 */
@Component
class OIDCUtil {
    @Autowired
    private lateinit var environment: Environment

    val subject: String?
        get() = TokenContextHolder.getClaimAsString("sub")

    fun autentisertBruker(): String = subject ?: jwtError("Fant ikke subject")

    fun jwtError(message: String): Nothing = throw JwtTokenInvalidException(message)

    fun getClaim(claim: String): String =
        if (erDevProfil()) {
            TokenContextHolder.getClaimAsString(claim) ?: "DEV_$claim"
        } else {
            TokenContextHolder.getClaimAsString(claim) ?: jwtError("Fant ikke claim '$claim' i tokenet")
        }

    fun getClaimAsList(claim: String): List<String>? =
        if (erDevProfil()) {
            listOf("group1")
        } else {
            TokenContextHolder.getClaimAsStringList(claim)
        }

    val navIdent: String
        get() =
            if (erDevProfil()) {
                "TEST_Z123"
            } else {
                TokenContextHolder.getClaimAsString("NAVident") ?: jwtError("Fant ikke NAVident")
            }

    val groups: List<String>?
        get() = TokenContextHolder.getClaimAsStringList("groups")

    val expiryDate: Date?
        get() = TokenContextHolder.getExpiry()?.let { Date.from(it) }

    private fun erDevProfil(): Boolean =
        environment.activeProfiles.any {
            listOf("dev", "mock-auth").contains(it.trim(' '))
        }
}
