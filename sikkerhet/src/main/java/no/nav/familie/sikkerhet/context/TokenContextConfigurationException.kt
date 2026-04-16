package no.nav.familie.sikkerhet.context

/**
 * Kastes av [TokenContextValidationAutoConfiguration] ved oppstart
 * hvis ingen eller mer enn én [TokenContext]-bean er registrert.
 */
class TokenContextConfigurationException(
    message: String,
) : IllegalStateException(message)
