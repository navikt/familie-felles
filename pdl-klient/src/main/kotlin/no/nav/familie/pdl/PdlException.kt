package no.nav.familie.pdl

open class PdlException(
    message: String = "Kall mot PDL feilet",
    val errors: List<PdlFeil> = emptyList(),
    val httpStatus: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    val feilmelding: String
        get() =
            message +
                errors
                    .mapNotNull { it.message?.takeIf(String::isNotBlank) }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = ": ", separator = "; ")
                    .orEmpty()
}
