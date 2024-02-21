package no.nav.familie.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.util.Date

@Component
class OIDCUtil(private val ctxHolder: TokenValidationContextHolder) {
    @Autowired
    private lateinit var environment: Environment

    val subject: String?
        get() = claimSet().subject

    fun autentisertBruker(): String {
        return subject ?: jwtError("Fant ikke subject")
    }

    fun jwtError(message: String): Nothing {
        throw JwtTokenValidatorException(message)
    }

    fun getClaim(claim: String): String {
        return if (erDevProfil()) {
            claimSet().get(claim)?.toString() ?: "DEV_$claim"
        } else {
            claimSet().get(claim)?.toString() ?: jwtError("Fant ikke claim '$claim' i tokenet")
        }
    }

    fun getClaimAsList(claim: String): List<String>? {
        return if (erDevProfil()) listOf("group1") else claimSet().getAsList(claim)
    }

    val navIdent: String
        get() =
            if (erDevProfil()) {
                "TEST_Z123"
            } else {
                claimSet().get("NAVident")?.toString() ?: jwtError("Fant ikke NAVident")
            }

    val groups: List<String>?
        get() =
            (claimSet().get("groups") as List<*>?)
                ?.filterNotNull()
                ?.map { it.toString() }

    fun claimSet(): JwtTokenClaims {
        return context().getClaims("azuread")
    }

    private fun context(): TokenValidationContext {
        return ctxHolder.getTokenValidationContext()
    }

    val expiryDate: Date?
        get() = claimSet()?.expirationTime

    private fun erDevProfil(): Boolean {
        return environment.activeProfiles.any {
            listOf("dev", "mock-auth").contains(it.trim(' '))
        }
    }
}
