package no.nav.familie.felles.tokenklient.entraid

import no.nav.familie.felles.tokenklient.TokenHenter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class EntraIDClient(
    @param:Value("\${NAIS_TOKEN_ENDPOINT}") private val tokenEndpoint: String,
    @param:Value("\${NAIS_TOKEN_EXCHANGE_ENDPOINT}") private val tokenExchangeEndpoint: String,
) {
    private val restClient: RestClient = RestClient.create()

    fun hentMaskinTilMaskinToken(scope: String): String {
        val body =
            mapOf(
                "identity_provider" to "entra_id",
                "target" to scope,
            )
        return TokenHenter.hentToken(restClient, tokenEndpoint, body)
    }

    fun hentOboToken(
        scope: String,
        brukerToken: String,
    ): String {
        val body =
            mapOf(
                "identity_provider" to "entra_id",
                "target" to scope,
                "user_token" to brukerToken,
            )
        return TokenHenter.hentToken(restClient, tokenExchangeEndpoint, body)
    }
}
