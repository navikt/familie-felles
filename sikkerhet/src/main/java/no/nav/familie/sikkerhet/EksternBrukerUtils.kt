package no.nav.familie.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestContextHolder


object EksternBrukerUtils {

    const val ISSUER = "selvbetjening"

    private val TOKEN_VALIDATION_CONTEXT_ATTRIBUTE = SpringTokenValidationContextHolder::class.java.name

    fun hentFnrFraToken(): String =
            claims()?.subject ?: throw JwtTokenValidatorException("Fant ikke subject")

    fun personIdentErLikInnloggetBruker(personIdent: String): Boolean =
            personIdent == hentFnrFraToken()

    fun getBearerTokenForLoggedInUser(): String =
            getTokenValidationContext().getJwtToken(ISSUER).tokenAsString

    private fun claims(): JwtTokenClaims? {
        val validationContext = getTokenValidationContext()
        return validationContext.getClaims(ISSUER)
    }

    private fun getTokenValidationContext(): TokenValidationContext {
        return RequestContextHolder.currentRequestAttributes()
                .getAttribute(TOKEN_VALIDATION_CONTEXT_ATTRIBUTE, 0) as TokenValidationContext
    }

}
