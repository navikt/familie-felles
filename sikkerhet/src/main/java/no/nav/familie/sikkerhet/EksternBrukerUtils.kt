package no.nav.familie.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestContextHolder

object EksternBrukerUtils {

    const val ISSUER = "selvbetjening"
    const val ISSUER_TOKENX = "tokenx"

    private val TOKEN_VALIDATION_CONTEXT_ATTRIBUTE = SpringTokenValidationContextHolder::class.java.name

    fun hentFnrFraToken(): String =
            claims()?.subject ?: throw JwtTokenValidatorException("Fant ikke subject")

    fun personIdentErLikInnloggetBruker(personIdent: String): Boolean =
            personIdent == hentFnrFraToken()

    fun getBearerTokenForLoggedInUser(): String {
        return getFromContext { validationContext, issuer ->
            validationContext.getJwtToken(issuer).tokenAsString
        }
    }

    private fun claims(): JwtTokenClaims? {
        return getFromContext { validationContext, issuer ->
            validationContext.getClaims(issuer)
        }
    }

    private fun <T> getFromContext(fn: (TokenValidationContext, String) -> T): T {
        val validationContext = getTokenValidationContext()
        return when {
            validationContext.hasTokenFor(ISSUER) -> fn.invoke(validationContext, ISSUER)
            validationContext.hasTokenFor(ISSUER_TOKENX) -> fn.invoke(validationContext, ISSUER_TOKENX)
            else -> error("Finner ikke token for ekstern bruker - issuers=${validationContext.issuers}")
        }
    }

    private fun getTokenValidationContext(): TokenValidationContext {
        return RequestContextHolder.currentRequestAttributes()
                .getAttribute(TOKEN_VALIDATION_CONTEXT_ATTRIBUTE, 0) as TokenValidationContext
    }

}
