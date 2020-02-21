package no.nav.familie.http.interceptor

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties

/**
 * Subklasse av BearerTokenClientInterceptor, som kun har clientProperties av grantType CLIENT_CREDENTIALS.
 * Brukes i prosjekter som gjør kall med både CLIENT_CREDENTIALS og JWT_BEARER mot samme base uri.
 */
class ClientCredentialsInterceptor(oAuth2AccessTokenService: OAuth2AccessTokenService,
                                   clientConfigurationProperties: ClientConfigurationProperties) :
        BearerTokenClientInterceptor(oAuth2AccessTokenService, clientConfigurationProperties) {

    override fun grantTypeFilter(clientProperties: ClientProperties): Boolean {
        return clientProperties.grantType == OAuth2GrantType.CLIENT_CREDENTIALS
    }
}
