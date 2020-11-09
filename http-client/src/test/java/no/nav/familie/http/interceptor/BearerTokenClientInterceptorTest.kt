package no.nav.familie.http.interceptor


import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import java.net.URI

class BearerTokenClientInterceptorTest {

    private lateinit var bearerTokenClientInterceptor: BearerTokenClientInterceptor

    private val oAuth2AccessTokenService = mockk<OAuth2AccessTokenService>(relaxed = true )

    @BeforeEach
    fun setup() {
        bearerTokenClientInterceptor = BearerTokenClientInterceptor(oAuth2AccessTokenService,
                                                                    clientConfigurationProperties)
    }

    @Test
    fun `intercept bruker grant type client credentials når det ikke er noen request context`() {
        val req = mockk<HttpRequest>(relaxed = true, relaxUnitFun = true)
        every { req.uri } returns(URI("http://firstResource.no"))
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)

        bearerTokenClientInterceptor.intercept(req, ByteArray(0), execution)

        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["1"]) }
    }

    private val clientConfigurationProperties =
            ClientConfigurationProperties(
                    mapOf("1" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.CLIENT_CREDENTIALS,
                                                  listOf("z", "y", "x"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 null),
                                                  URI("http://firstResource.no"), null),
                          "2" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.JWT_BEARER,
                                                  listOf("c", "b", "a"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 null),
                                                  URI("http://firstResource.no"), null),
                          "3" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.CLIENT_CREDENTIALS,
                                                  listOf("z", "y", "x"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 null),
                                                  URI("http://secondResource.no"), null))
            )
}
