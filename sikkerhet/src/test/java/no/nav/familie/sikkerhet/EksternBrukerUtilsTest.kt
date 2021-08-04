package no.nav.familie.sikkerhet

import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

internal class EksternBrukerUtilsTest {

    private val selvbetjening =
            "selvbetjening" to
                    JwtToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzIiwiaWF0IjoxfQ.jy6mWElItzueUS6xk2tOjAry_hnDckZ0kOiwDru0fss")
    private val tokenx =
            "tokenx" to JwtToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0IiwiaWF0IjoxfQ.zu8o7u62R-Z4RjE851TVpXqTViiA6Z4mpXnJ68L64iU")
    private val annetToken =
            "annetToken" to JwtToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0IiwiaWF0IjoxfQ.zu8o7u62R-Z4RjE851TVpXqTViiA6Z4mpXnJ68L64iU")

    @AfterEach
    internal fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    internal fun `skal hente selvbetjening hvis den finnes`() {
        mockRequestAttributes(mapOf(selvbetjening, tokenx))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo("s")
    }

    @Test
    internal fun `skal hente tokenx hvis ikke selvbetjening finnes`() {
        mockRequestAttributes(mapOf(tokenx))
        assertThat(EksternBrukerUtils.hentFnrFraToken()).isEqualTo("t")
    }

    @Test
    internal fun `skal kaste feil hvis selvbetjening eller tokenx finnes`() {
        mockRequestAttributes(mapOf(annetToken))
        assertThat(catchThrowable { EksternBrukerUtils.hentFnrFraToken() })
                .hasMessage("Finner ikke token for ekstern bruker - issuers=[annetToken]")
    }

    private fun mockRequestAttributes(issuerShortNameValidatedTokenMap: Map<String, JwtToken>) {
        val requestAttributes = mockk<RequestAttributes>()
        every { requestAttributes.getAttribute(any(), any()) } returns TokenValidationContext(issuerShortNameValidatedTokenMap)
        RequestContextHolder.setRequestAttributes(requestAttributes)
    }
}
