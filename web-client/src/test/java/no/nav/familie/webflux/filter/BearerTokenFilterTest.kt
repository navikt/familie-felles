package no.nav.familie.webflux.filter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.sikkerhet.context.TokenContext
import no.nav.familie.sikkerhet.context.TokenContextTestHelper
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFunction
import java.net.URI

class BearerTokenFilterTest {
    private lateinit var bearerTokenFilter: BearerTokenFilter

    private val oAuth2AccessTokenService = mockk<OAuth2AccessTokenService>(relaxed = true)
    private val tokenContext = mockk<TokenContext>()

    @BeforeEach
    fun setup() {
        TokenContextTestHelper.setContext(tokenContext)
        every { tokenContext.getClaimAsString(any(), any()) } returns null
        bearerTokenFilter =
            BearerTokenFilter(
                oAuth2AccessTokenService,
                clientConfigurationProperties,
            )
    }

    @AfterEach
    internal fun tearDown() {
        TokenContextTestHelper.clearContext()
    }

    @Test
    fun `intercept bruker grant type client credentials når det ikke er noen request context`() {
        val req = mockk<ClientRequest>(relaxed = true, relaxUnitFun = true)
        every { req.url() } returns (URI("http://firstResource.no"))
        val execution = mockk<ExchangeFunction>(relaxed = true)

        bearerTokenFilter.filter(req, execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["1"]!!) }
    }

    @Test
    fun `intercept bruker grant type jwt token når det finnes saksbehandler context`() {
        every { tokenContext.getClaimAsString("preferred_username", any()) } returns "saksbehandler"

        val req = mockk<ClientRequest>(relaxed = true, relaxUnitFun = true)
        every { req.url() } returns (URI("http://firstResource.no"))
        val execution = mockk<ExchangeFunction>(relaxed = true)

        bearerTokenFilter.filter(req, execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["2"]!!) }
    }
}
