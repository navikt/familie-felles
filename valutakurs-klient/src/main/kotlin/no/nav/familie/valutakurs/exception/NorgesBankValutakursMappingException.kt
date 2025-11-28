package no.nav.familie.valutakurs.exception

sealed class NorgesBankValutakursMappingException(
    msg: String,
    cause: Throwable?,
) : RuntimeException("Feil ved mapping av valutakursdata fra Norges Bank. $msg", cause) {
    class UventetAntall(
        msg: String,
        cause: Throwable? = null,
    ) : NorgesBankValutakursMappingException(msg, cause)

    class ManglerFelt(
        msg: String,
        cause: Throwable? = null,
    ) : NorgesBankValutakursMappingException(msg, cause)

    class UgyldigData(
        msg: String,
        cause: Throwable? = null,
    ) : NorgesBankValutakursMappingException(msg, cause)
}
