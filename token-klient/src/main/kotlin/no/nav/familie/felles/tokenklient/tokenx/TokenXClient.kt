package no.nav.familie.felles.tokenklient.tokenx

import no.nav.familie.felles.tokenklient.TokenHenter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TokenXClient(
    @param:Value("\${NAIS_TOKEN_EXCHANGE_ENDPOINT}") private val tokenEndpoint: String,
) {
    private val restClient = RestClient.create()

    fun hentToken(
        scope: String,
        tokenValue: String,
    ): String {
        val body =
            mapOf(
                "identity_provider" to "tokenx",
                "target" to scope,
                "user_token" to tokenValue,
            )
        return TokenHenter.hentToken(restClient, tokenEndpoint, body)
    }
}
