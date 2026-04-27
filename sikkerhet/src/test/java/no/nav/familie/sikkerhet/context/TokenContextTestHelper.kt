package no.nav.familie.sikkerhet.context

/**
 * Testhjelpeklasse som gir tilgang til [TokenContextHolder.setContext] og [TokenContextHolder.clearContext].
 *
 * Tilgjengelig via test-jar for andre moduler som trenger å sette opp [TokenContext] i tester.
 */
object TokenContextTestHelper {
    fun setContext(tokenContext: TokenContext) = TokenContextHolder.setContext(tokenContext)

    fun clearContext() = TokenContextHolder.clearContext()
}
