package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Oppgave(
    val id: Long,
    val tildeltEnhetsnr: String,
    val tema: String,
    val oppgavetype: String,
    val versjon: Int,
    val prioritet: Prioritet,
    val status: Oppgavestatus,
    val aktivDato: LocalDate,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val aktoerId: String? = null,
    val orgnr: String? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    val mappeId: Long? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val opprettetTidspunkt: OffsetDateTime? = null,
    val ferdigstiltTidspunkt: OffsetDateTime? = null,
    val endretTidspunkt: OffsetDateTime? = null,
    val bruker: Bruker? = null,
    val beskrivelseAvOppgaven: String? = null,
    val identer: List<Ident>? = null,
    val metadata: Map<MetadataKey, String>? = null,
    val kommentar: RegistrerKommentar? = null,
) {
    override fun toString(): String = "Oppgave"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bruker(
    val ident: String? = null,
    val type: Brukertype? = null,
) {
    override fun toString(): String = "Bruker"
}
