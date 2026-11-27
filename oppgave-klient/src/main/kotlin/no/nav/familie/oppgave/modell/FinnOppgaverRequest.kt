package no.nav.familie.oppgave.modell

import jakarta.validation.constraints.Max
import java.time.LocalDate
import java.time.LocalDateTime

data class FinnOppgaverRequest(
    val statuskategori: Oppgavestatuskategori? = null,
    val status: List<Oppgavestatus>? = null,
    val tema: List<String>? = null,
    val oppgavetype: List<String>? = null,
    val aktoerId: List<String>? = null,
    val journalpostId: List<String>? = null,
    val saksreferanse: List<String>? = null,
    val orgnr: List<String>? = null,
    val tildeltEnhetsnr: String? = null,
    val tildeltRessurs: Boolean? = null,
    val tilordnetRessurs: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val erUtenMappe: Boolean? = null,
    val mappeId: List<Long>? = null,
    val opprettetAv: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val aktivDatoFom: LocalDate? = null,
    val aktivDatoTom: LocalDate? = null,
    val opprettetFom: LocalDateTime? = null,
    val opprettetTom: LocalDateTime? = null,
    val ferdigstiltFom: LocalDateTime? = null,
    val ferdigstiltTom: LocalDateTime? = null,
    val fristFom: LocalDate? = null,
    val fristTom: LocalDate? = null,
    val behandlesAvApplikasjon: String? = null,
    val bnr: List<String>? = null,
    val samhandlernr: List<String>? = null,
    val metadatanokkel: MetadataKey? = null,
    val metadataverdi: List<String>? = null,
    val sorteringsrekkefolge: Sorteringsrekkefolge? = null,
    val sorteringsfelt: Sorteringsfelt? = null,
    @field:Max(1000)
    val limit: Long? = null,
    val offset: Long? = null,
    val presisMatchTemakategorisering: Boolean? = null,
) {
    override fun toString(): String = "FinnOppgaverRequest"
}
