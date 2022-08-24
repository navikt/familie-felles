package no.nav.familie.http.ecb

class ECBClientException(override val message: String, override val cause: Throwable?) : RuntimeException(message, cause)
