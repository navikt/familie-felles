package no.nav.familie.kontrakt

import java.io.PrintWriter
import java.io.StringWriter

data class Ressurs<T>(
        val data: T?,
        val status: Status,
        val melding: String,
        val stacktrace: String?

) {

    enum class Status { SUKSESS, FEILET, IKKE_HENTET, IKKE_TILGANG }

    companion object {

        fun <T> success(data: T, melding: String? = null): Ressurs<T> =
                Ressurs(data,
                                                                   Status.SUKSESS,
                                                                   melding ?: "Innhenting av data var vellykket",
                                                                   null)

        fun <T> failure(errorMessage: String? = null, error: Throwable? = null): Ressurs<T> =
                Ressurs(null,
                                                                   Status.FEILET,
                                                                   errorMessage ?: "Kunne ikke hente data: ${error?.message}",
                                                                   error?.textValue())

        fun <T> ikkeTilgang(melding: String): Ressurs<T> =
                Ressurs(null,
                                                                   Status.IKKE_TILGANG,
                                                                   melding,
                                                                   null)

        private fun Throwable.textValue(): String {
            val sw = StringWriter()
            this.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }
    }

}

