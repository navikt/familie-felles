package no.nav.familie.sak.kontrakt

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

        fun <T> success(data: T, melding: String? = null): Ressurs<T> = Ressurs(
                data = data,
                status = Status.SUKSESS,
                melding = melding ?: "Innhenting av data var vellykket",
                stacktrace = null
        )

        fun <T> failure(errorMessage: String? = null, error: Throwable? = null): Ressurs<T> = Ressurs(
                data = null,
                status = Status.FEILET,
                melding = errorMessage ?: "Kunne ikke hente data: ${error?.message}",
                stacktrace = error?.textValue()
        )

        fun <T> ikkeTilgang(melding: String): Ressurs<T> = Ressurs(
                data = null,
                status = Status.IKKE_TILGANG,
                melding = melding,
                stacktrace = null
        )

        private fun Throwable.textValue(): String {
            val sw = StringWriter()
            this.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }
    }

}

