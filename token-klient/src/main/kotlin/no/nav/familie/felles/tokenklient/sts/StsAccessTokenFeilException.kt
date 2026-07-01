package no.nav.familie.felles.tokenklient.sts

class StsAccessTokenFeilException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
