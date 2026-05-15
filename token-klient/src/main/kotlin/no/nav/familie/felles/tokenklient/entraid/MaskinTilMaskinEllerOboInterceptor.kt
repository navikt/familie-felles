package no.nav.familie.felles.tokenklient.entraid

import com.nimbusds.jwt.JWTParser
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * Interceptor som gir ClientCredentials-token hvis ingen token er tilgjengelig eller tokenet ikke har
 * brukernavn, ellers OBO-token. Dette for å støtte funksjonalitet hvor asynkrone prosesser som ikke
 * har en innlogget bruker.
 *
 * Det anbefales å ha et mer bevist forhold til om man skal ha maskin-til-maskin eller OBO, og heller velge disse
 * interceptorene direkte.
 */
class MaskinTilMaskinEllerOboInterceptor(
    private val entraIDClient: EntraIDClient,
    private val target: String,
    private val tokenSupplier: () -> String?,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val token = tokenSupplier()
        val accessToken =
            if (token != null && harPreferredUsername(token)) {
                entraIDClient.hentOboToken(target, token)
            } else {
                entraIDClient.hentMaskinTilMaskinToken(target)
            }
        request.headers.setBearerAuth(accessToken)
        return execution.execute(request, body)
    }

    private fun harPreferredUsername(token: String): Boolean =
        try {
            JWTParser.parse(token).jwtClaimsSet.getStringClaim("preferred_username") != null
        } catch (e: Exception) {
            false
        }
}
