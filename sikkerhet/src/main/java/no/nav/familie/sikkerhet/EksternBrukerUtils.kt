package no.nav.familie.sikkerhet

import no.nav.familie.sikkerhet.context.TokenContextHolder

object EksternBrukerUtils {
    const val ISSUER_SELVBETJENING = "selvbetjening"
    const val ISSUER_TOKENX = "tokenx"

    private val FNR_REGEX = """[0-9]{11}""".toRegex()

    /**
     * Henter fødselsnummer fra `pid`- eller `sub`-claimet i tokenet til innlogget ekstern bruker.
     *
     * Kaster [JwtTokenInvalidException] hvis kallet ikke skjer i kontekst av en ekstern bruker.
     */
    fun hentFnrFraToken(): String {
        val issuer = resolveIssuer()
        val fnr =
            (
                TokenContextHolder.getClaimAsString("pid", issuer)
                    ?: TokenContextHolder.getClaimAsString("sub", issuer)
                    ?: throw JwtTokenInvalidException("Finner ikke sub/pid på token")
            )
        if (!FNR_REGEX.matches(fnr)) {
            throw JwtTokenInvalidException("Ugyldig fødselsnummer")
        }
        return fnr
    }

    /** Returnerer true hvis [personIdent] er lik fødselsnummeret i innlogget brukers token. */
    fun personIdentErLikInnloggetBruker(personIdent: String): Boolean = personIdent == hentFnrFraToken()

    /**
     * Henter bearer token for innlogget ekstern bruker (tokenx eller selvbetjening).
     *
     * Kaster [JwtTokenInvalidException] hvis kallet ikke skjer i kontekst av en ekstern bruker.
     */
    fun getBearerTokenForLoggedInUser(): String {
        val issuer = resolveIssuer()
        return TokenContextHolder.getBearerToken(issuer)
            ?: throw JwtTokenInvalidException("Klarte ikke hente token fra issuer $issuer")
    }

    private fun resolveIssuer(): String =
        when {
            TokenContextHolder.hasTokenFor(ISSUER_SELVBETJENING) -> ISSUER_SELVBETJENING
            TokenContextHolder.hasTokenFor(ISSUER_TOKENX) -> ISSUER_TOKENX
            else -> throw JwtTokenInvalidException("Finner ikke token for ekstern bruker - issuers=${TokenContextHolder.issuers()}")
        }
}
