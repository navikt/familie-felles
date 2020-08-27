package no.nav.familie.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestContextHolder


object EksternBrukerUtils {

    const val ISSUER = "selvbetjening"

    fun hentFnrFraToken(): String {
        return fødselsnummer
    }

    fun personIdentErLikInnloggetBruker(personIdent: String): Boolean {
        return personIdent == hentFnrFraToken()
    }

    fun getBearerTokenForLoggedInUser(): String {
        return getTokenValidationContext().getJwtToken(ISSUER).tokenAsString
    }

    private val TOKEN_VALIDATION_CONTEXT_ATTRIBUTE = SpringTokenValidationContextHolder::class.java.name

    private val subject: String?
        get() = claims()?.subject

    private val fødselsnummer: String
        get() = subject ?: throw JwtTokenValidatorException("Fant ikke subject")

    private fun claims(): JwtTokenClaims? {
        val validationContext = getTokenValidationContext()
        return validationContext.getClaims(ISSUER)
    }

    private fun getTokenValidationContext(): TokenValidationContext {
        return RequestContextHolder.currentRequestAttributes().getAttribute(getContextHolderName(), 0) as TokenValidationContext
    }

    private fun getContextHolderName(): String {
        val holder = "no.nav.security.token.support.spring.SpringTokenValidationContextHolder"
        return TOKEN_VALIDATION_CONTEXT_ATTRIBUTE ?: holder
    }
}
