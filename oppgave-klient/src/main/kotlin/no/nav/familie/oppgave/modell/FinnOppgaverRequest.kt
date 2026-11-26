package no.nav.familie.oppgave.modell

import java.time.LocalDateTime

data class FinnOppgaverRequest(
    val statuskategori: String? = null,
    val tema: List<String>? = null,
    val oppgavetype: List<String>? = null,
    val aktoerId: List<String>? = null,
    val journalpostId: List<String>? = null,
    val saksreferanse: List<String>? = null,
    val orgnr: List<String>? = null,
    val tildeltEnhetsnr: String? = null,
    val tilordnetRessurs: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val ferdigstiltFom: LocalDateTime? = null,
    val ferdigstiltTom: LocalDateTime? = null,
    val limit: Long? = null,
    val offset: Long? = null,
)
