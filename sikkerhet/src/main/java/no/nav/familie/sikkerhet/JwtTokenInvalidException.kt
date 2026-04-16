package no.nav.familie.sikkerhet

/**
 * Kastes når JWT-token mangler forventede claims eller inneholder ugyldig data.
 */
class JwtTokenInvalidException(
    message: String,
) : RuntimeException(message)
