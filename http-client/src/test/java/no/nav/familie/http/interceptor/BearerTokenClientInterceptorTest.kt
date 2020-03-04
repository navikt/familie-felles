package no.nav.familie.http.interceptor


import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.junit.Before
import org.junit.Test
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
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
        val tokenValidationContext = mockk<TokenValidationContext>()
        every { tokenValidationContext.getClaims("azuread") }
                .returns(JwtTokenClaims(JWTClaimsSet.Builder()
                                                .issuer("azure")
                                                .claim("preferred_username", "bob")
                                                .build()))
        SpringTokenValidationContextHolder().tokenValidationContext = mockk()
        val req = mockk<HttpRequest>()
        every { req.uri }.returns(URI("http://firstResource.no"))
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)
        every { execution.execute(any(), any()) }.returns(mockk())

        bearerTokenClientInterceptor.intercept(req, ByteArray(0), execution)


        verify { oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["2"]) }

    }


    private val clientConfigurationProperties =
            ClientConfigurationProperties(
                    mapOf("1" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.CLIENT_CREDENTIALS,
                                                  listOf("z", "y", "x"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 null),
                                                  URI("http://firstResource.no")),
                          "2" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.JWT_BEARER,
                                                  listOf("c", "b", "a"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 null),
                                                  URI("http://firstResource.no")),
                          "3" to ClientProperties(URI("http://tokenendpoint.com"),
                                                  OAuth2GrantType.CLIENT_CREDENTIALS,
                                                  listOf("z", "y", "x"),
                                                  ClientAuthenticationProperties("clientIdent",
                                                                                 ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                                                                 "Secrets are us",
                                                                                 null),
                                                  URI("http://secondResource.no")))
            )


}
