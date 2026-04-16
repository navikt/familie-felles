package no.nav.familie.sikkerhet.context

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

internal class TokenContextHolderTest {
    private val mockContext = mockk<TokenContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        TokenContextHolder.setContext(mockContext)
    }

    @AfterEach
    fun tearDown() {
        TokenContextHolder.clearContext()
    }

    @Test
    fun `getContext henter konteksten som setContext satt`() {
        assertThat(TokenContextHolder.getContext()).isSameAs(mockContext)
    }

    @Test
    fun `getClaimAsString henter fra kontekst`() {
        every { mockContext.getClaimAsString("sub", "issuer") } returns "12345"
        assertThat(TokenContextHolder.getClaimAsString("sub", "issuer")).isEqualTo("12345")
    }

    @Test
    fun `hasTokenFor henter fra kontekst`() {
        every { mockContext.hasTokenFor("issuer") } returns true
        assertThat(TokenContextHolder.hasTokenFor("issuer")).isTrue()
    }

    @Test
    fun `getBearerToken henter fra kontekst`() {
        every { mockContext.getBearerToken("issuer") } returns "bearer.token"
        assertThat(TokenContextHolder.getBearerToken("issuer")).isEqualTo("bearer.token")
    }

    @Test
    fun `issuers henter fra kontekst`() {
        every { mockContext.issuers() } returns listOf("azuread", "tokenx")
        assertThat(TokenContextHolder.issuers()).containsExactly("azuread", "tokenx")
    }

    @Test
    fun `getExpiry henter fra kontekst`() {
        val expiry = Instant.now().plusSeconds(3600)
        every { mockContext.getExpiry("azuread") } returns expiry
        assertThat(TokenContextHolder.getExpiry("azuread")).isEqualTo(expiry)
    }

    @Test
    fun `getContext kaster TokenContextConfigurationException når ingen kontekst er satt`() {
        TokenContextHolder.clearContext()
        assertThatThrownBy { TokenContextHolder.getContext() }
            .isInstanceOf(TokenContextConfigurationException::class.java)
            .hasMessageContaining("Ingen TokenContext er konfigurert")
    }
}
