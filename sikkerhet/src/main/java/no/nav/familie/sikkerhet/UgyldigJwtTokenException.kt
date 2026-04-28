package no.nav.familie.sikkerhet

/**
 * Kastes når JWT-token mangler forventede claims eller inneholder ugyldig data.
 */
class UgyldigJwtTokenException(
    message: String,
) : RuntimeException(message)
