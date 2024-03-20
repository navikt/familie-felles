package no.nav.familie.valutakurs.exception

sealed class ValutakursClientException(override val message: String, override val cause: Throwable?) : RuntimeException(message, cause)

class ValutakursException(override val message: String, override val cause: Throwable?) : ValutakursClientException(message, cause)

class IngenValutakursException(override val message: String, override val cause: Throwable?) : ValutakursClientException(message, cause)
