package no.nav.familie.webflux.filter

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import java.net.URI

private val tokenEndpoint = "http://tokenendpoint.com"
private val authentication =
    ClientAuthenticationProperties(
        "clientIdent",
        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
        "Secrets are us",
        null,
    )
val clientConfigurationProperties =
    ClientConfigurationProperties(
        mapOf(
            "1" to
                ClientProperties(
                    URI(tokenEndpoint),
                    URI(tokenEndpoint),
                    OAuth2GrantType.CLIENT_CREDENTIALS,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://firstResource.no"),
                    null,
                ),
            "2" to
                ClientProperties(
                    URI(tokenEndpoint),
                    URI(tokenEndpoint),
                    OAuth2GrantType.JWT_BEARER,
                    listOf("c", "b", "a"),
                    authentication,
                    URI("http://firstResource.no"),
                    null,
                ),
            "3" to
                ClientProperties(
                    URI(tokenEndpoint),
                    URI(tokenEndpoint),
                    OAuth2GrantType.JWT_BEARER,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://jwtResource.no"),
                    null,
                ),
            "4" to
                ClientProperties(
                    URI(tokenEndpoint),
                    URI(tokenEndpoint),
                    OAuth2GrantType.CLIENT_CREDENTIALS,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://clientResource.no"),
                    null,
                ),
        ),
    )
