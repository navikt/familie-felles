package no.nav.familie.saf

class SafException(
    message: String = "Kall mot Saf feilet",
    val errors: List<SafFeil> = emptyList(),
    val httpStatus: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
