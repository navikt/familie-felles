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
