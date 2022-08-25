package no.nav.familie.http.ecb.exception

class ECBClientException(override val message: String, override val cause: Throwable?) : RuntimeException(message, cause)
