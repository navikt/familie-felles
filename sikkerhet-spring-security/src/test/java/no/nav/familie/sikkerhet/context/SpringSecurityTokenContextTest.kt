package no.nav.familie.sikkerhet.context

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

internal class SpringSecurityTokenContextTest {
    private val azureadIssuerUrl = "https://azuread"
    private val tokenxIssuerUrl = "https://tokenx"
    private val selvbetjeningIssuerUrl = "https://selvbetjening"

    private val tokenContext =
        SpringSecurityTokenContext(
            issuerNameMapping =
                mapOf(
                    azureadIssuerUrl to "azuread",
                    tokenxIssuerUrl to "tokenx",
                    selvbetjeningIssuerUrl to "selvbetjening",
                ),
        )

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `hasTokenFor returnerer true for selvbetjening-token via name mapping`() {
        mockJwt(selvbetjeningIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.hasTokenFor("azuread")).isFalse()
        assertThat(tokenContext.hasTokenFor("tokenx")).isFalse()
        assertThat(tokenContext.hasTokenFor("selvbetjening")).isTrue()
    }

    @Test
    fun `hasTokenFor returnerer true for tokenx-token via name mapping`() {
        mockJwt(tokenxIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.hasTokenFor("azuread")).isFalse()
        assertThat(tokenContext.hasTokenFor("tokenx")).isTrue()
        assertThat(tokenContext.hasTokenFor("selvbetjening")).isFalse()
    }

    @Test
    fun `hasTokenFor returnerer true for azuread via name mapping`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.hasTokenFor("azuread")).isTrue()
        assertThat(tokenContext.hasTokenFor("tokenx")).isFalse()
        assertThat(tokenContext.hasTokenFor("selvbetjening")).isFalse()
    }

    @Test
    fun `hasTokenFor returnerer true når jwt matcher issuer-URL direkte`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.hasTokenFor(azureadIssuerUrl)).isTrue()
    }

    @Test
    fun `hasTokenFor returnerer false når det ikke finnes et token`() {
        assertThat(tokenContext.hasTokenFor("azuread")).isFalse()
    }

    @Test
    fun `hasTokenFor returnerer false når tokenet ikke er JwtAuthenticationToken`() {
        val auth = mockk<Authentication>()
        SecurityContextHolder.getContext().authentication = auth
        assertThat(tokenContext.hasTokenFor("azuread")).isFalse()
    }

    @Test
    fun `getClaimAsString returnerer claim-verdien når den finnes`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123", "NAVident" to "Z999999"))
        assertThat(tokenContext.getClaimAsString("NAVident", "azuread")).isEqualTo("Z999999")
    }

    @Test
    fun `getClaimAsString returnerer null når claimet mangler`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.getClaimAsString("missing", "azuread")).isNull()
    }

    @Test
    fun `getClaimAsString returnerer null når det ikke finnes token`() {
        assertThat(tokenContext.getClaimAsString("sub", "azuread")).isNull()
    }

    @Test
    fun `getClaimAsString returnerer null når issuer ikke stemmer`() {
        mockJwt(tokenxIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.getClaimAsString("sub", "azuread")).isNull()
    }

    @Test
    fun `getBearerToken returnerer rå token-verdi`() {
        val tokenValue = "my.jwt.token"
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123"), tokenValue = tokenValue)
        assertThat(tokenContext.getBearerToken("azuread")).isEqualTo(tokenValue)
    }

    @Test
    fun `getBearerToken returnerer null når det ikke finnes token`() {
        assertThat(tokenContext.getBearerToken("azuread")).isNull()
    }

    @Test
    fun `issuers returnerer issuer-URL`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.issuers()).containsExactly(azureadIssuerUrl)
    }

    @Test
    fun `issuers returnerer tom liste når det ikke finnes token`() {
        assertThat(tokenContext.issuers()).isEmpty()
    }

    @Test
    fun `getClaimAsStringList returnerer liste for List-claim`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123", "roles" to listOf("role1", "role2")))
        assertThat(tokenContext.getClaimAsStringList("roles", "azuread")).containsExactly("role1", "role2")
    }

    @Test
    fun `getClaimAsStringList returnerer liste med ett element for String-claim`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123", "roles" to "access_as_application"))
        assertThat(tokenContext.getClaimAsStringList("roles", "azuread")).containsExactly("access_as_application")
    }

    @Test
    fun `getClaimAsStringList returnerer null når claimet mangler`() {
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.getClaimAsStringList("roles", "azuread")).isNull()
    }

    @Test
    fun `getExpiry returnerer utløpstidspunktet for tokenet`() {
        val expiresAt = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        mockJwt(azureadIssuerUrl, mapOf("sub" to "user123"), expiresAt = expiresAt)
        assertThat(tokenContext.getExpiry("azuread")).isEqualTo(expiresAt)
    }

    @Test
    fun `getExpiry returnerer null når issuer ikke stemmer`() {
        mockJwt(tokenxIssuerUrl, mapOf("sub" to "user123"))
        assertThat(tokenContext.getExpiry("azuread")).isNull()
    }

    @Test
    fun `getExpiry returnerer null når det ikke finnes token`() {
        assertThat(tokenContext.getExpiry("azuread")).isNull()
    }

    private fun mockJwt(
        issuerUrl: String,
        claims: Map<String, Any>,
        tokenValue: String = "dummy.jwt.token",
        expiresAt: Instant = Instant.now().plusSeconds(3600),
    ) {
        val jwt =
            Jwt(
                tokenValue,
                Instant.now(),
                expiresAt,
                mapOf("alg" to "RS256"),
                claims + ("iss" to issuerUrl),
            )
        val auth = JwtAuthenticationToken(jwt)
        SecurityContextHolder.getContext().authentication = auth
    }
}
