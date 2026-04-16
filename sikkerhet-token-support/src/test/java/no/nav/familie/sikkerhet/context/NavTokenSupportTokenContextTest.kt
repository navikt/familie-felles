package no.nav.familie.sikkerhet.context

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.time.Instant

internal class NavTokenSupportTokenContextTest {
    private val tokenContext = NavTokenSupportTokenContext()

    private val azureadToken =
        "azuread" to JwtToken("eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMTExMTExMTExMSIsInByZWZlcnJlZF91c2VybmFtZSI6InVzZXJAbmF2Lm5vIn0.")
    private val tokenxToken = "tokenx" to JwtToken("eyJhbGciOiJub25lIn0.eyJzdWIiOiIyMjIyMjIyMjIyMiIsInBpZCI6IjIyMjIyMjIyMjIyIn0.")

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `hasTokenFor returnerer true når token finnes for issueren`() {
        mockContext(mapOf(azureadToken))
        assertThat(tokenContext.hasTokenFor("azuread")).isTrue()
        assertThat(tokenContext.hasTokenFor("tokenx")).isFalse()
    }

    @Test
    fun `hasTokenFor returnerer false når det ikke finnes en request context`() {
        assertThat(tokenContext.hasTokenFor("azuread")).isFalse()
    }

    @Test
    fun `issuers returnerer alle issuere med token`() {
        mockContext(mapOf(azureadToken, tokenxToken))
        assertThat(tokenContext.issuers()).containsExactlyInAnyOrder("azuread", "tokenx")
    }

    @Test
    fun `issuers returnerer tom liste når det ikke finnes en request context`() {
        assertThat(tokenContext.issuers()).isEmpty()
    }

    @Test
    fun `getClaimAsString returnerer claim-verdien når den finnes`() {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("user123")
                .claim("NAVident", "Z999999")
                .build()
        val context = mockk<TokenValidationContext>()
        every { context.getClaims("azuread") } returns JwtTokenClaims(claims)
        every { context.issuers } returns listOf("azuread")
        mockContextHolder(context)
        assertThat(tokenContext.getClaimAsString("NAVident", "azuread")).isEqualTo("Z999999")
    }

    @Test
    fun `getClaimAsString returnerer null når claimet mangler`() {
        val claims = JWTClaimsSet.Builder().subject("user123").build()
        val context = mockk<TokenValidationContext>()
        every { context.getClaims("azuread") } returns JwtTokenClaims(claims)
        every { context.issuers } returns listOf("azuread")
        mockContextHolder(context)
        assertThat(tokenContext.getClaimAsString("missing", "azuread")).isNull()
    }

    @Test
    fun `getClaimAsString returnerer null når det ikke finnes en request context`() {
        assertThat(tokenContext.getClaimAsString("sub", "azuread")).isNull()
    }

    @Test
    fun `getBearerToken returnerer null når det ikke finnes en request context`() {
        assertThat(tokenContext.getBearerToken("azuread")).isNull()
    }

    @Test
    fun `getClaimAsStringList returnerer liste for List-claim`() {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("user123")
                .claim("roles", listOf("role1", "role2"))
                .build()
        val context = mockk<TokenValidationContext>()
        every { context.getClaims("azuread") } returns JwtTokenClaims(claims)
        every { context.issuers } returns listOf("azuread")
        mockContextHolder(context)
        assertThat(tokenContext.getClaimAsStringList("roles", "azuread")).containsExactly("role1", "role2")
    }

    @Test
    fun `getClaimAsStringList returnerer liste med ett element for String-claim`() {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("user123")
                .claim("roles", "access_as_application")
                .build()
        val context = mockk<TokenValidationContext>()
        every { context.getClaims("azuread") } returns JwtTokenClaims(claims)
        every { context.issuers } returns listOf("azuread")
        mockContextHolder(context)
        assertThat(tokenContext.getClaimAsStringList("roles", "azuread")).containsExactly("access_as_application")
    }

    @Test
    fun `getClaimAsStringList returnerer null når claimet mangler`() {
        val claims = JWTClaimsSet.Builder().subject("user123").build()
        val context = mockk<TokenValidationContext>()
        every { context.getClaims("azuread") } returns JwtTokenClaims(claims)
        every { context.issuers } returns listOf("azuread")
        mockContextHolder(context)
        assertThat(tokenContext.getClaimAsStringList("roles", "azuread")).isNull()
    }

    @Test
    fun `getExpiry returnerer utløpstidspunktet for tokenet`() {
        val expiry = java.util.Date.from(Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("user123")
                .expirationTime(expiry)
                .build()
        val context = mockk<TokenValidationContext>()
        every { context.getClaims("azuread") } returns JwtTokenClaims(claims)
        every { context.issuers } returns listOf("azuread")
        mockContextHolder(context)
        assertThat(tokenContext.getExpiry("azuread")).isEqualTo(expiry.toInstant())
    }

    @Test
    fun `getExpiry returnerer null når claimet ikke finnes`() {
        val claims = JWTClaimsSet.Builder().subject("user123").build()
        val context = mockk<TokenValidationContext>()
        every { context.getClaims("azuread") } returns JwtTokenClaims(claims)
        every { context.issuers } returns listOf("azuread")
        mockContextHolder(context)
        assertThat(tokenContext.getExpiry("azuread")).isNull()
    }

    @Test
    fun `getExpiry returnerer null når det ikke finnes en request context`() {
        assertThat(tokenContext.getExpiry("azuread")).isNull()
    }

    private fun mockContext(issuerTokenMap: Map<String, JwtToken>) {
        val context = TokenValidationContext(issuerTokenMap)
        mockContextHolder(context)
    }

    private fun mockContextHolder(context: TokenValidationContext) {
        val requestAttributes = mockk<RequestAttributes>()
        every {
            requestAttributes.getAttribute(
                SpringTokenValidationContextHolder::class.java.name,
                RequestAttributes.SCOPE_REQUEST,
            )
        } returns context
        RequestContextHolder.setRequestAttributes(requestAttributes)
    }
}
