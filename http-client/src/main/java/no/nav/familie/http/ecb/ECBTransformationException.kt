package no.nav.familie.http.ecb

class ECBTransformationException(override val message: String, override val cause: Throwable?) : RuntimeException(message, cause)
