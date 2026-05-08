package no.nav.familie.felles.texas

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TexasClient(
    @param:Value("\${NAIS_TOKEN_ENDPOINT}") private val tokenEndpoint: String,
) {
    private val restClient = RestClient.create()

    fun hentMaskinToken(scope: String): String {
        val body =
            mapOf(
                "identity_provider" to "entra_id",
                "target" to scope,
            )
        val response =
            restClient
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TexasTokenResponse::class.java)
                ?: throw IllegalStateException("Fikk ikke svar fra Texas")
        return response.accessToken
    }
}
