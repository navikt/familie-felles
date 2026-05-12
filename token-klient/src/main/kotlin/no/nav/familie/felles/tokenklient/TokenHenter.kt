package no.nav.familie.felles.tokenklient

import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

internal object TokenHenter {
    fun hentToken(
        restClient: RestClient,
        tokenEndpoint: String,
        body: Map<String, String>,
    ): String {
        val response =
            restClient
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TokenResponse::class.java)
                ?: throw IllegalStateException("Fikk ikke svar fra token-endpoint")
        return response.accessToken
    }
}
