package no.nav.familie.http.interceptor


import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.mockk.mockk
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.junit.Before
import org.junit.Test
import java.net.URI

class BearerTokenClientInterceptorTest {

    private lateinit var bearerTokenClientInterceptor: BearerTokenClientInterceptor

    private val oAuth2AccessTokenService = mockk<OAuth2AccessTokenService>()

    @Before
    fun setup() {
        bearerTokenClientInterceptor = BearerTokenClientInterceptor(oAuth2AccessTokenService,
                                                                    clientConfigurationProperties)
    }

    @Test
    fun `intercept`() {
    }


    private val clientConfigurationProperties =
            ClientConfigurationProperties(
                    mapOf("1" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.CLIENT_CREDENTIALS,
                                                  listOf("z", "y", "x"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 ""),
                                                  URI("http://firstResource.no")),
                          "2" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.JWT_BEARER,
                                                  listOf("c", "b", "a"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 ""),
                                                  URI("http://firstResource.no")),
                          "3" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.CLIENT_CREDENTIALS,
                                                  listOf("z", "y", "x"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 ""),
                                                  URI("http://secondResource.no")))
            )


}
