package no.nav.familie.http.interceptor

import no.nav.familie.http.sts.StsRestClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.beans.factory.support.SecurityContextProvider
import org.springframework.boot.actuate.endpoint.SecurityContext
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

import java.net.URI

@Component
class BearerTokenWithSTSFallbackClientInterceptor(oAuth2AccessTokenService: OAuth2AccessTokenService,
                                                  clientConfigurationProperties: ClientConfigurationProperties,
                                                  private val stsRestClient: StsRestClient) :
        BearerTokenClientInterceptor(oAuth2AccessTokenService, clientConfigurationProperties) {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        if (preferredUsername() == null) {
            request.headers.setBearerAuth(stsRestClient.systemOIDCToken)
        } else {
            return super.intercept(request, body, execution)
        }
        return execution.execute(request, body)
    }
    private fun preferredUsername(): Any? {
        return try {
            SpringTokenValidationContextHolder().tokenValidationContext.getClaims("azuread")["preferred_username"]
        } catch (e: Throwable) {
            // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
            null
        }
    }
}
