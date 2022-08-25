package no.nav.familie.http.ecb.exception

class ECBTransformationException(override val message: String, override val cause: Throwable?) : RuntimeException(message, cause)
