package no.nav.familie.http.interceptor

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import java.net.URI

internal class BearerTokenClientCredentialsClientInterceptorTest {
    private lateinit var bearerTokenClientInterceptor: BearerTokenClientCredentialsClientInterceptor

    private val oAuth2AccessTokenService = mockk<OAuth2AccessTokenService>(relaxed = true)

    @BeforeEach
    fun setup() {
        bearerTokenClientInterceptor =
            BearerTokenClientCredentialsClientInterceptor(
                oAuth2AccessTokenService,
                clientConfigurationProperties,
            )
    }

    @Test
    fun `intercept bruker grant type client credentials`() {
        val req = mockk<HttpRequest>(relaxed = true, relaxUnitFun = true)
        every { req.uri } returns (URI("http://firstResource.no"))
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)

        bearerTokenClientInterceptor.intercept(req, ByteArray(0), execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration.get("1")!!) }
    }

    @Test
    fun `intercept skal kaste feil når det ikke finnes en property med gyldig grant type`() {
        val req = mockk<HttpRequest>(relaxed = true, relaxUnitFun = true)
        every { req.uri } returns (URI("http://jwtResource.no"))
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)
        assertThat(catchThrowable { bearerTokenClientInterceptor.intercept(req, ByteArray(0), execution) })
            .hasMessage(
                "could not find oauth2 client config for " +
                    "uri=http://jwtResource.no and grant type=client_credentials",
            )
    }
}
