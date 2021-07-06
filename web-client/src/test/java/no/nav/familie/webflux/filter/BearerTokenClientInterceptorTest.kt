package no.nav.familie.webflux.filter


import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFunction
import java.net.URI

class BearerTokenClientInterceptorTest {

    private lateinit var bearerTokenFilterFunction: BearerTokenFilterFunction

    private val oAuth2AccessTokenService = mockk<OAuth2AccessTokenService>(relaxed = true)

    @BeforeEach
    fun setup() {
        bearerTokenFilterFunction = BearerTokenFilterFunction(oAuth2AccessTokenService,
                                                              clientConfigurationProperties)
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    fun `intercept bruker grant type client credentials når det ikke er noen request context`() {
        val req = mockk<ClientRequest>(relaxed = true, relaxUnitFun = true)
        every { req.url() } returns (URI("http://firstResource.no"))
        val execution = mockk<ExchangeFunction>(relaxed = true)

        bearerTokenFilterFunction.filter(req, execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["1"]) }
    }

    @Test
    fun `intercept bruker grant type jwt token når det finnes saksbehandler context`() {
        mockBrukerContext("saksbehandler")

        val req = mockk<ClientRequest>(relaxed = true, relaxUnitFun = true)
        every { req.url() } returns (URI("http://firstResource.no"))
        val execution = mockk<ExchangeFunction>(relaxed = true)

        bearerTokenFilterFunction.filter(req, execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["2"]) }
    }

    private fun mockBrukerContext(preferredUsername: String) {
        val tokenValidationContext = mockk<TokenValidationContext>()
        val jwtTokenClaims = mockk<JwtTokenClaims>()
        val requestAttributes = mockk<RequestAttributes>()
        RequestContextHolder.setRequestAttributes(requestAttributes)
        every {
            requestAttributes.getAttribute(SpringTokenValidationContextHolder::class.java.name,
                                           RequestAttributes.SCOPE_REQUEST)
        } returns tokenValidationContext
        every { tokenValidationContext.getClaims("azuread") } returns jwtTokenClaims
        every { jwtTokenClaims.get("preferred_username") } returns preferredUsername
        every { jwtTokenClaims.get("NAVident") } returns preferredUsername
    }

    private fun clearBrukerContext() {
        RequestContextHolder.resetRequestAttributes()
    }
}
