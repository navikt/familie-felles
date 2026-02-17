package no.nav.familie.restklient.sts

class StsAccessTokenFeilException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
