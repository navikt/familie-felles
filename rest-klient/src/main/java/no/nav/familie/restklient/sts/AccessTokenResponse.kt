package no.nav.familie.restklient.sts

class AccessTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
)
