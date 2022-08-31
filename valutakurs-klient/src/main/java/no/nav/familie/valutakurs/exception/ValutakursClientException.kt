package no.nav.familie.valutakurs.exception

class ValutakursClientException(override val message: String, override val cause: Throwable?) : RuntimeException(message, cause)
