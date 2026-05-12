package no.nav.familie.felles.tokenklient

import com.fasterxml.jackson.annotation.JsonProperty

data class TexasTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val utløperOm: Int,
    @JsonProperty("token_type")
    val tokenType: String,
)
