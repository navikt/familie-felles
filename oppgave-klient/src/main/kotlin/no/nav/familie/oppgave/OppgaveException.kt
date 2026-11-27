package no.nav.familie.oppgave

enum class OppgaveOperasjon {
    FINN_OPPGAVER,
    OPPRETT_OPPGAVE,
    HENT_OPPGAVE,
    PATCH_OPPGAVE,
}

enum class OppgaveFeiltype {
    HTTP,
    TRANSPORT,
    UGYLDIG_RESPONS,
}

/**
 * Kan inneholde sensitive opplysninger i message, cause og stacktrace.
 * Klienten logger ikke exceptionen automatisk. Ikke serialiser den som et svar til sluttbrukeren.
 */
class OppgaveException(
    val operasjon: OppgaveOperasjon,
    val feiltype: OppgaveFeiltype,
    val httpStatus: Int?,
    val korrelasjonsId: String,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(
        message ?: (
            "Oppgave-kall feilet: operasjon=$operasjon, feiltype=$feiltype, " +
                "httpStatus=$httpStatus, korrelasjonsId=$korrelasjonsId"
        ),
        cause,
    )
