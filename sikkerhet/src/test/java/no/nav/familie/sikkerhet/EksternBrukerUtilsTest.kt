package no.nav.familie.sikkerhet

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

internal class EksternBrukerUtilsTest {
    private val selvbetjening = "selvbetjening" to JwtToken("eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMTExMTExMTExMSJ9.")
    private val tokenx = "tokenx" to JwtToken("eyJhbGciOiJub25lIn0.eyJzdWIiOiIyMjIyMjIyMjIyMiJ9.")
    private val annetToken = "annetToken" to JwtToken("eyJhbGciOiJub25lIn0.eyJzdWIiOiIyMjIyMjIyMjIyMiJ9.")

    private val sub: String = "11111111111"
    private val pid: String = "22222222222"

    @AfterEach
    internal fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    internal fun `skal hente selvbetjening hvis den finnes`() {
        mockRequestAttributes(mapOf(selvbetjening, tokenx))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(sub)
    }

    @Test
    internal fun `skal hente tokenx hvis ikke selvbetjening finnes`() {
        mockRequestAttributes(mapOf(tokenx))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(pid)
    }

    @Test
    internal fun `skal kaste feil hvis selvbetjening eller tokenx ikke finnes`() {
        mockRequestAttributes(mapOf(annetToken))
        assertThat(catchThrowable { EksternBrukerUtils.hentFnrFraToken() })
            .hasMessage("Finner ikke token for ekstern bruker - issuers=[annetToken]")
    }

    @Test
    internal fun `skal kaste feil hvis det ikke finnes en token`() {
        mockRequestAttributes(emptyMap())
        assertThatThrownBy { EksternBrukerUtils.hentFnrFraToken() }
            .hasMessage("Finner ikke token for ekstern bruker - issuers=[]")
    }

    @Test
    internal fun `skal kaste feil hvis det ikke finnes subject eller pid`() {
        setContextHolder()
        assertThatThrownBy { EksternBrukerUtils.hentFnrFraToken() }
            .hasMessage("Finner ikke sub/pid på token")
    }

    @Test
    internal fun `returnerer subject hvis ikke pid finnes`() {
        setContextHolder(sub = sub)
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(sub)
    }

    @Test
    internal fun `returnerer pid hvis pid og sub finnes finnes`() {
        setContextHolder(sub = sub, pid = pid)
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(pid)
    }

    @Test
    internal fun `returnerer pid hvis kun pid finnes`() {
        setContextHolder(pid = pid)
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo(pid)
    }

    @Test
    internal fun `skal feile hvis sub ikke er 11 siffer langt`() {
        listOf("1", "123456789012", "abcdefghijk").forEach {
            assertThatThrownBy {
                setContextHolder(sub = it)
                EksternBrukerUtils.hentFnrFraToken()
            }.hasMessageContaining("er ikke gyldig fnr")
        }
    }

    @Test
    internal fun `skal feile hvis pid ikke er 11 siffer langt`() {
        listOf("1", "123456789012", "abcdefghijk").forEach {
            assertThatThrownBy {
                setContextHolder(pid = it)
                EksternBrukerUtils.hentFnrFraToken()
            }.hasMessageContaining("er ikke gyldig fnr")
        }
    }

    private fun setContextHolder(
        sub: String? = null,
        pid: String? = null,
    ) {
        val builder = JWTClaimsSet.Builder()
        sub?.let { builder.subject(it) }
        pid?.let { builder.claim("pid", it) }
        val tokenValidationContext = mockk<TokenValidationContext>()
        every { tokenValidationContext.getClaims("selvbetjening") } returns JwtTokenClaims(builder.build())
        every { tokenValidationContext.hasTokenFor("selvbetjening") } returns true
        mockAttributes(tokenValidationContext)
    }

    private fun mockRequestAttributes(issuerShortNameValidatedTokenMap: Map<String, JwtToken>) {
        val tokenValidationContext = TokenValidationContext(issuerShortNameValidatedTokenMap)
        mockAttributes(tokenValidationContext)
    }

    private fun mockAttributes(tokenValidationContext: TokenValidationContext) {
        val requestAttributes = mockk<RequestAttributes>()
        every { requestAttributes.getAttribute(any(), any()) } returns tokenValidationContext
        RequestContextHolder.setRequestAttributes(requestAttributes)
    }
}
