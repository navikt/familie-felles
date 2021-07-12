package no.nav.familie.sikkerhet

import no.nav.familie.sikkerhet.EksternBrukerIssuer.SELVBETJENING
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestContextHolder

enum class EksternBrukerIssuer(val issuer: String) {
    SELVBETJENING(EksternBrukerUtils.ISSUER),
    TOKEN_X("tokenx")
}

object EksternBrukerUtils {

    const val ISSUER = "selvbetjening"

    private val TOKEN_VALIDATION_CONTEXT_ATTRIBUTE = SpringTokenValidationContextHolder::class.java.name

    fun hentFnrFraToken(issuer: EksternBrukerIssuer = SELVBETJENING): String =
            claims(issuer)?.subject ?: throw JwtTokenValidatorException("Fant ikke subject")

    fun personIdentErLikInnloggetBruker(personIdent: String, issuer: EksternBrukerIssuer = SELVBETJENING): Boolean =
            personIdent == hentFnrFraToken(issuer)

    fun getBearerTokenForLoggedInUser(issuer: EksternBrukerIssuer = SELVBETJENING): String =
            getTokenValidationContext().getJwtToken(issuer.issuer).tokenAsString

    private fun claims(issuer: EksternBrukerIssuer = SELVBETJENING): JwtTokenClaims? {
        val validationContext = getTokenValidationContext()
        return validationContext.getClaims(issuer.issuer)
    }

    private fun getTokenValidationContext(): TokenValidationContext {
        return RequestContextHolder.currentRequestAttributes()
                .getAttribute(TOKEN_VALIDATION_CONTEXT_ATTRIBUTE, 0) as TokenValidationContext
    }

}
