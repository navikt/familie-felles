package no.nav.familie.restklient.client

/**
 * Kastes når en REST-klient mottar et svar med en null-body. Ignorer i klient hvis null-body er forventet.
 */
class ResponseBodyNullException(
    message: String,
) : NullPointerException(message)
