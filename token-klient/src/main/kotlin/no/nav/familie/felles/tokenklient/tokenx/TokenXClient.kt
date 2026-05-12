package no.nav.familie.felles.tokenklient.tokenx

import no.nav.familie.felles.tokenklient.TokenResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
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
        val response =
            restClient
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TokenResponse::class.java)
                ?: throw IllegalStateException("Fikk ikke svar fra token-exchange endpoint")

        return response.accessToken
    }
}
