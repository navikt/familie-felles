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
        request.headers.setBearerAuth(genererAccessToken(request, clientConfigurationProperties, oAuth2AccessTokenService))
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
            request.headers.setBearerAuth(genererAccessToken(request, clientConfigurationProperties, oAuth2AccessTokenService))
        }
        return execution.execute(request, body)
    }
}

private fun genererAccessToken(request: HttpRequest,
                               clientConfigurationProperties: ClientConfigurationProperties,
                               oAuth2AccessTokenService: OAuth2AccessTokenService) : String {
    val clientProperties = clientPropertiesFor(request.uri, clientConfigurationProperties)
    val response: OAuth2AccessTokenResponse = oAuth2AccessTokenService.getAccessToken(clientProperties)
    return response.accessToken
}

private fun clientPropertiesFor(uri: URI, clientConfigurationProperties: ClientConfigurationProperties): ClientProperties {
    val values = clientConfigurationProperties
            .registration
            .values
            .filter { uri.toString().startsWith(it.resourceUrl.toString()) }
    return if (values.size == 1) values.first() else filterForGrantType(values, uri)
}

private fun filterForGrantType(values: List<ClientProperties>, uri: URI): ClientProperties {
    val grantType = if (erSystembruker()) OAuth2GrantType.CLIENT_CREDENTIALS else OAuth2GrantType.JWT_BEARER
    return values.firstOrNull { grantType == it.grantType }
            ?: error("could not find oauth2 client config for uri=$uri and grant type=$grantType")
}

private fun erSystembruker(): Boolean {
    return try {
        SpringTokenValidationContextHolder().tokenValidationContext.getClaims("azuread")["preferred_username"]
        false
    } catch (e: Throwable) {
        // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
        true
    }
}

