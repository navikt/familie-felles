package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class PatchOppgaveRequest(
    val versjon: Int,
    val orgnr: PatchFelt<String> = PatchFelt.Utelatt,
    val status: PatchFelt<String> = PatchFelt.Utelatt,
    val endretAvEnhetsnr: PatchFelt<String> = PatchFelt.Utelatt,
    val beskrivelse: PatchFelt<String> = PatchFelt.Utelatt,
    val tilordnetRessurs: PatchFelt<String> = PatchFelt.Utelatt,
    val tildeltEnhetsnr: PatchFelt<String> = PatchFelt.Utelatt,
    val prioritet: PatchFelt<String> = PatchFelt.Utelatt,
    val behandlingstema: PatchFelt<String> = PatchFelt.Utelatt,
    val behandlingstype: PatchFelt<String> = PatchFelt.Utelatt,
    val oppgavetype: PatchFelt<String> = PatchFelt.Utelatt,
    val tema: PatchFelt<String> = PatchFelt.Utelatt,
    val journalpostId: PatchFelt<String> = PatchFelt.Utelatt,
    val saksreferanse: PatchFelt<String> = PatchFelt.Utelatt,
    val behandlesAvApplikasjon: PatchFelt<String> = PatchFelt.Utelatt,
    val personident: PatchFelt<String> = PatchFelt.Utelatt,
    val fristFerdigstillelse: PatchFelt<LocalDate> = PatchFelt.Utelatt,
    val aktivDato: PatchFelt<LocalDate> = PatchFelt.Utelatt,
    val kommentar: PatchFelt<RegistrerKommentarJson> = PatchFelt.Utelatt,
)
