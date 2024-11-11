package no.nav.familie.webflux.sts

class StsAccessTokenFeilException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
