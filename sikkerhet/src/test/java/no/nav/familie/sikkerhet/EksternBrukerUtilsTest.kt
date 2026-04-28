package no.nav.familie.sikkerhet

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.sikkerhet.context.TokenContext
import no.nav.familie.sikkerhet.context.TokenContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EksternBrukerUtilsTest {
    private val sub: String = "11111111111"
    private val pid: String = "22222222222"

    private val mockContext = mockk<TokenContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        TokenContextHolder.setContext(mockContext)
    }

    @AfterEach
    fun tearDown() {
        TokenContextHolder.clearContext()
    }

    private fun configureContext(
        hasSelvbetjening: Boolean = false,
        hasTokenx: Boolean = false,
        issuers: Collection<String> = emptyList(),
        selvbetjeningClaims: Map<String, Any?> = emptyMap(),
        tokenxClaims: Map<String, Any?> = emptyMap(),
        selvbetjeningBearer: String? = null,
        tokenxBearer: String? = null,
    ) {
        every { mockContext.hasTokenFor("selvbetjening") } returns hasSelvbetjening
        every { mockContext.hasTokenFor("tokenx") } returns hasTokenx
        every { mockContext.issuers() } returns issuers
        every { mockContext.getClaimAsString(any(), "selvbetjening") } answers { selvbetjeningClaims[firstArg()] as? String }
        every { mockContext.getClaimAsString(any(), "tokenx") } answers { tokenxClaims[firstArg()] as? String }
        every { mockContext.getBearerToken("selvbetjening") } returns selvbetjeningBearer
        every { mockContext.getBearerToken("tokenx") } returns tokenxBearer
    }

    @Test
    internal fun `skal hente selvbetjening hvis den finnes`() {
        configureContext(hasSelvbetjening = true, hasTokenx = true, selvbetjeningClaims = mapOf("sub" to sub))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(sub)
    }

    @Test
    internal fun `skal hente tokenx hvis ikke selvbetjening finnes`() {
        configureContext(hasTokenx = true, tokenxClaims = mapOf("sub" to pid))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(pid)
    }

    @Test
    internal fun `skal kaste feil hvis selvbetjening eller tokenx ikke finnes`() {
        configureContext(issuers = listOf("annetToken"))
        assertThat(catchThrowable { EksternBrukerUtils.hentFnrFraToken() })
            .isInstanceOf(UgyldigJwtTokenException::class.java)
            .hasMessage("Finner ikke token for ekstern bruker - issuers=[annetToken]")
    }

    @Test
    internal fun `skal kaste feil hvis det ikke finnes en token`() {
        configureContext(issuers = emptyList())
        assertThatThrownBy { EksternBrukerUtils.hentFnrFraToken() }
            .isInstanceOf(UgyldigJwtTokenException::class.java)
            .hasMessage("Finner ikke token for ekstern bruker - issuers=[]")
    }

    @Test
    internal fun `skal kaste feil hvis det ikke finnes subject eller pid`() {
        configureContext(hasSelvbetjening = true)
        assertThatThrownBy { EksternBrukerUtils.hentFnrFraToken() }
            .isInstanceOf(UgyldigJwtTokenException::class.java)
            .hasMessage("Finner ikke sub/pid på token")
    }

    @Test
    internal fun `returnerer subject hvis ikke pid finnes`() {
        configureContext(hasSelvbetjening = true, selvbetjeningClaims = mapOf("sub" to sub))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(sub)
    }

    @Test
    internal fun `returnerer pid hvis pid og sub finnes finnes`() {
        configureContext(hasSelvbetjening = true, selvbetjeningClaims = mapOf("sub" to sub, "pid" to pid))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(pid)
    }

    @Test
    internal fun `returnerer pid hvis kun pid finnes`() {
        configureContext(hasSelvbetjening = true, selvbetjeningClaims = mapOf("pid" to pid))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(pid)
    }

    @Test
    internal fun `skal feile hvis sub ikke er 11 siffer langt`() {
        listOf("1", "123456789012", "abcdefghijk").forEach { invalid ->
            configureContext(hasSelvbetjening = true, selvbetjeningClaims = mapOf("sub" to invalid))
            assertThatThrownBy { EksternBrukerUtils.hentFnrFraToken() }
                .isInstanceOf(UgyldigJwtTokenException::class.java)
                .hasMessage("Ugyldig fødselsnummer")
        }
    }

    @Test
    internal fun `skal feile hvis pid ikke er 11 siffer langt`() {
        listOf("1", "123456789012", "abcdefghijk").forEach { invalid ->
            configureContext(hasSelvbetjening = true, selvbetjeningClaims = mapOf("pid" to invalid))
            assertThatThrownBy { EksternBrukerUtils.hentFnrFraToken() }
                .isInstanceOf(UgyldigJwtTokenException::class.java)
                .hasMessage("Ugyldig fødselsnummer")
        }
    }

    @Test
    internal fun `getBearerTokenForLoggedInUser returnerer token for selvbetjening`() {
        configureContext(hasSelvbetjening = true, selvbetjeningBearer = "my.token")
        assertThat(EksternBrukerUtils.getBearerTokenForLoggedInUser()).isEqualTo("my.token")
    }

    @Test
    internal fun `personIdentErLikInnloggetBruker returnerer true når ident matcher`() {
        configureContext(hasSelvbetjening = true, selvbetjeningClaims = mapOf("sub" to sub))
        assertThat(EksternBrukerUtils.personIdentErLikInnloggetBruker(sub)).isTrue()
        assertThat(EksternBrukerUtils.personIdentErLikInnloggetBruker("99999999999")).isFalse()
    }
}
