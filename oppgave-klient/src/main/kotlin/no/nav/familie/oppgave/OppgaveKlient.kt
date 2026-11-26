package no.nav.familie.oppgave

import no.nav.familie.oppgave.modell.FinnOppgaverRequest
import no.nav.familie.oppgave.modell.Oppgave
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import no.nav.familie.oppgave.modell.PatchOppgaveRequest
import no.nav.familie.oppgave.modell.SokOppgaverResponse

interface OppgaveKlient {
    fun finnOppgaver(
        request: FinnOppgaverRequest,
        korrelasjonsId: String? = null,
    ): SokOppgaverResponse

    /**
     * Oppretter en oppgave, eller henter den eksisterende oppgaven ved UUID-duplikat (HTTP 409 med oppgaveId).
     * Konsumenten må gjenbruke samme request-UUID for samme logiske oppretting ved retry og etter omstart.
     * Serveren sammenligner bare UUID, ikke requestinnhold. En gjenfunnet oppgave returneres i sin nåværende tilstand.
     * Klienten gjentar ikke POST automatisk. Feil ved gjenfinning sendes videre som feil fra GET-kallet.
     */
    fun opprettOppgave(
        request: OpprettOppgaveRequest,
        korrelasjonsId: String? = null,
    ): Oppgave

    fun hentOppgave(
        id: Long,
        korrelasjonsId: String? = null,
    ): Oppgave

    fun patchOppgave(
        id: Long,
        request: PatchOppgaveRequest,
        korrelasjonsId: String? = null,
    ): Oppgave
}
