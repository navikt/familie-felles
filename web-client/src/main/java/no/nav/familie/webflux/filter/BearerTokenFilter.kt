package no.nav.familie.webflux.filter

import no.nav.familie.webflux.sts.StsTokenClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.net.URI

interface BearerTokenFilterFunction : ExchangeFilterFunction

@Component
class BearerTokenFilter(
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val clientConfigurationProperties: ClientConfigurationProperties
) : BearerTokenFilterFunction {

    override fun filter(request: ClientRequest, function: ExchangeFunction): Mono<ClientResponse> {
        return retrieveAndAddBearerToken(
            oAuth2AccessTokenService,
            clientConfigurationProperties,
            request,
            function,
            null
        )
    }
}

@Component
class BearerTokenClientCredentialFilter(
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val clientConfigurationProperties: ClientConfigurationProperties
) : BearerTokenFilterFunction {

    override fun filter(request: ClientRequest, function: ExchangeFunction): Mono<ClientResponse> {
        return retrieveAndAddBearerToken(
            oAuth2AccessTokenService,
            clientConfigurationProperties,
            request,
            function,
            OAuth2GrantType.CLIENT_CREDENTIALS
        )
    }
}

@Component
class BearerTokenOnBehalfOfFilter(
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val clientConfigurationProperties: ClientConfigurationProperties
) : BearerTokenFilterFunction {

    override fun filter(request: ClientRequest, function: ExchangeFunction): Mono<ClientResponse> {
        return retrieveAndAddBearerToken(
            oAuth2AccessTokenService,
            clientConfigurationProperties,
            request,
            function,
            OAuth2GrantType.JWT_BEARER
        )
    }
}

@Component
class BearerTokenExchangeFilter(
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val clientConfigurationProperties: ClientConfigurationProperties
) : BearerTokenFilterFunction {

    override fun filter(request: ClientRequest, function: ExchangeFunction): Mono<ClientResponse> {
        return retrieveAndAddBearerToken(
            oAuth2AccessTokenService,
            clientConfigurationProperties,
            request,
            function,
            OAuth2GrantType.TOKEN_EXCHANGE
        )
    }
}

@Import(StsTokenClient::class)
@Component
class BearerTokenStsFallbackFilter(
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val stsRestClient: StsTokenClient
) : BearerTokenFilterFunction {

    override fun filter(request: ClientRequest, function: ExchangeFunction): Mono<ClientResponse> {
        return if (erSystembruker()) {
            addBearerToken(request, function, stsRestClient.systemOIDCToken)
        } else {
            retrieveAndAddBearerToken(
                oAuth2AccessTokenService,
                clientConfigurationProperties,
                request,
                function
            )
        }
    }
}

private fun retrieveAndAddBearerToken(
    oAuth2AccessTokenService: OAuth2AccessTokenService,
    clientConfigurationProperties: ClientConfigurationProperties,
    request: ClientRequest,
    function: ExchangeFunction,
    grantType: OAuth2GrantType? = null
): Mono<ClientResponse> {
    val accessToken = genererAccessToken(
        request,
        clientConfigurationProperties,
        oAuth2AccessTokenService,
        grantType
    )
    return addBearerToken(request, function, accessToken)
}

private fun addBearerToken(
    request: ClientRequest,
    function: ExchangeFunction,
    accessToken: String
): Mono<ClientResponse> {
    val modifiedRequest = ClientRequest.from(request).header(
        "Authorization",
        "Bearer " + accessToken
    ).build()
    return function.exchange(modifiedRequest)
}

private fun genererAccessToken(
    request: ClientRequest,
    clientConfigurationProperties: ClientConfigurationProperties,
    oAuth2AccessTokenService: OAuth2AccessTokenService,
    grantType: OAuth2GrantType? = null
): String {
    val clientProperties = clientPropertiesFor(
        request.url(),
        clientConfigurationProperties,
        grantType
    )
    return oAuth2AccessTokenService.getAccessToken(clientProperties).accessToken
}

/**
 * Finds client property for grantType if specified.
 *
 * If the grantType isn't specified:
 *  - Returns first client property, if there is only one
 *  - Returns client property for client_credentials or jwt_bearer
 */
private fun clientPropertiesFor(
    uri: URI,
    clientConfigurationProperties: ClientConfigurationProperties,
    grantType: OAuth2GrantType?
): ClientProperties {
    val clientProperties = filterClientProperties(clientConfigurationProperties, uri)
    return if (grantType == null) {
        if (clientProperties.size == 1) {
            clientProperties.first()
        } else {
            clientPropertiesForGrantType(clientProperties, clientCredentialOrJwtBearer(), uri)
        }
    } else {
        clientPropertiesForGrantType(clientProperties, grantType, uri)
    }
}

private fun filterClientProperties(
    clientConfigurationProperties: ClientConfigurationProperties,
    uri: URI
) = clientConfigurationProperties
    .registration
    .values
    .filter { uri.toString().startsWith(it.resourceUrl.toString()) }

private fun clientPropertiesForGrantType(
    values: List<ClientProperties>,
    grantType: OAuth2GrantType,
    uri: URI
): ClientProperties {
    return values.firstOrNull { grantType == it.grantType }
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
