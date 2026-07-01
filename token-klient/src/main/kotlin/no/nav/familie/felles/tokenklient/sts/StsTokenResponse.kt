package no.nav.familie.felles.tokenklient.sts

import com.fasterxml.jackson.annotation.JsonProperty

data class StsTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
)
