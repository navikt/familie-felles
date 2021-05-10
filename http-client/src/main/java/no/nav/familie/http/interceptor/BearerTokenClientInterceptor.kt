package no.nav.familie.http.interceptor

import no.nav.familie.http.sts.StsRestClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.net.URI

@Component
class BearerTokenClientInterceptor(private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                                   private val clientConfigurationProperties: ClientConfigurationProperties) :
        ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        request.headers.setBearerAuth(genererAccessToken(request,
                                                         clientConfigurationProperties,
                                                         oAuth2AccessTokenService,
                                                         clientCredentialOrJwtBearer()))
        return execution.execute(request, body)
    }
}

@Component
class BearerTokenClientCredentialsClientInterceptor(private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                                                    private val clientConfigurationProperties: ClientConfigurationProperties) :
        ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        request.headers.setBearerAuth(genererAccessToken(request,
                                                         clientConfigurationProperties,
                                                         oAuth2AccessTokenService,
                                                         OAuth2GrantType.CLIENT_CREDENTIALS))
        return execution.execute(request, body)
    }
}

@Component
class BearerTokenOnBehalfOfClientInterceptor(private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                                             private val clientConfigurationProperties: ClientConfigurationProperties) :
        ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        request.headers.setBearerAuth(genererAccessToken(request,
                                                         clientConfigurationProperties,
                                                         oAuth2AccessTokenService,
                                                         OAuth2GrantType.JWT_BEARER))
        return execution.execute(request, body)
    }
}

@Component
class BearerTokenWithSTSFallbackClientInterceptor(private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                                                  private val clientConfigurationProperties: ClientConfigurationProperties,
                                                  private val stsRestClient: StsRestClient) :
        ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        if (erSystembruker()) {
            request.headers.setBearerAuth(stsRestClient.systemOIDCToken)
        } else {
            request.headers.setBearerAuth(genererAccessToken(request,
                                                             clientConfigurationProperties,
                                                             oAuth2AccessTokenService,
                                                             clientCredentialOrJwtBearer()))
        }
        return execution.execute(request, body)
    }
}

private fun genererAccessToken(request: HttpRequest,
                               clientConfigurationProperties: ClientConfigurationProperties,
                               oAuth2AccessTokenService: OAuth2AccessTokenService,
                               grantType: OAuth2GrantType): String {
    val clientProperties = clientPropertiesFor(request.uri,
                                               clientConfigurationProperties,
                                               grantType)
    val response: OAuth2AccessTokenResponse = oAuth2AccessTokenService.getAccessToken(clientProperties)
    return response.accessToken
}

private fun clientPropertiesFor(uri: URI,
                                clientConfigurationProperties: ClientConfigurationProperties,
                                grantType: OAuth2GrantType?): ClientProperties {
    return clientConfigurationProperties
                   .registration
                   .values
                   .filter { uri.toString().startsWith(it.resourceUrl.toString()) }
                   .firstOrNull { grantType == it.grantType }
           ?: error("could not find oauth2 client config for uri=$uri and grant type=$grantType")
}

private fun clientCredentialOrJwtBearer() =
        if (erSystembruker()) OAuth2GrantType.CLIENT_CREDENTIALS else OAuth2GrantType.JWT_BEARER

private fun erSystembruker(): Boolean {
    return try {
        val preferred_username =
                SpringTokenValidationContextHolder().tokenValidationContext.getClaims("azuread")["preferred_username"]
        return preferred_username == null
    } catch (e: Throwable) {
        // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
        true
    }
}
