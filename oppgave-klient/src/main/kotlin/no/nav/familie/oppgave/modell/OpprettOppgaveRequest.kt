package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettOppgaveRequest(
    val aktivDato: LocalDate,
    val oppgavetype: String,
    val prioritet: String,
    val tema: String,
    val personident: String? = null,
    val orgnr: String? = null,
    val samhandlernr: String? = null,
    val tildeltEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val beskrivelse: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val uuid: UUID? = null,
) {
    override fun toString(): String = "OpprettOppgaveRequest"
}
