package no.nav.familie.restklient.interceptor

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
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
                    GrantType.CLIENT_CREDENTIALS,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://firstResource.no"),
                    null,
                ),
            "2" to
                ClientProperties(
                    URI(tokenEndpoint),
                    URI(tokenEndpoint),
                    GrantType.JWT_BEARER,
                    listOf("c", "b", "a"),
                    authentication,
                    URI("http://firstResource.no"),
                    null,
                ),
            "3" to
                ClientProperties(
                    URI(tokenEndpoint),
                    URI(tokenEndpoint),
                    GrantType.JWT_BEARER,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://jwtResource.no"),
                    null,
                ),
            "4" to
                ClientProperties(
                    URI(tokenEndpoint),
                    URI(tokenEndpoint),
                    GrantType.CLIENT_CREDENTIALS,
                    listOf("z", "y", "x"),
                    authentication,
                    URI("http://clientResource.no"),
                    null,
                ),
        ),
    )
