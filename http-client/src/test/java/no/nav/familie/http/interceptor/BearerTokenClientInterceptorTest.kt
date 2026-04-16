package no.nav.familie.http.interceptor

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.sikkerhet.context.TokenContext
import no.nav.familie.sikkerhet.context.TokenContextConfigurationException
import no.nav.familie.sikkerhet.context.TokenContextHolder
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.test.util.ReflectionTestUtils
import java.net.URI

class BearerTokenClientInterceptorTest {
    private lateinit var bearerTokenClientInterceptor: BearerTokenClientInterceptor

    private val oAuth2AccessTokenService = mockk<OAuth2AccessTokenService>(relaxed = true)
    private val tokenContext = mockk<TokenContext>(relaxed = true)

    @BeforeEach
    fun setup() {
        ReflectionTestUtils.setField(TokenContextHolder, "context", tokenContext)
        bearerTokenClientInterceptor =
            BearerTokenClientInterceptor(
                oAuth2AccessTokenService,
                clientConfigurationProperties,
            )
    }

    @AfterEach
    fun tearDown() {
        ReflectionTestUtils.setField(TokenContextHolder, "context", null)
    }

    @Test
    fun `intercept bruker grant type client credentials når det ikke er noen request context`() {
        every { tokenContext.getClaimAsString("preferred_username", "azuread") } returns null

        val req = mockk<HttpRequest>(relaxed = true, relaxUnitFun = true)
        every { req.uri } returns (URI("http://firstResource.no"))
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)

        bearerTokenClientInterceptor.intercept(req, ByteArray(0), execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["1"]!!) }
    }

    @Test
    fun `intercept bruker grant type jwt token når det finnes saksbehandler context`() {
        every { tokenContext.getClaimAsString("preferred_username", "azuread") } returns "saksbehandler"

        val req = mockk<HttpRequest>(relaxed = true, relaxUnitFun = true)
        every { req.uri } returns (URI("http://firstResource.no"))
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)

        bearerTokenClientInterceptor.intercept(req, ByteArray(0), execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["2"]!!) }
    }

    @Test
    fun `erSystembruker kaster TokenContextConfigurationException videre når TokenContextHolder ikke er konfigurert`() {
        ReflectionTestUtils.setField(TokenContextHolder, "context", null)

        val req = mockk<HttpRequest>(relaxed = true, relaxUnitFun = true)
        every { req.uri } returns (URI("http://firstResource.no"))
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)

        assertThrows(TokenContextConfigurationException::class.java) {
            bearerTokenClientInterceptor.intercept(req, ByteArray(0), execution)
        }
    }
}
