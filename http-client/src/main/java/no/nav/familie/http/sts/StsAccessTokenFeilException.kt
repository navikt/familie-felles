package no.nav.familie.http.sts

class StsAccessTokenFeilException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
