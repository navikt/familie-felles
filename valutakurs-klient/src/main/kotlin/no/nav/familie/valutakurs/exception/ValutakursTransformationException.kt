package no.nav.familie.valutakurs.exception

class ValutakursTransformationException(
    override val message: String,
    override val cause: Throwable?,
) : RuntimeException(message, cause)
