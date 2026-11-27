package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SokOppgaverResponse(
    val antallTreffTotalt: Long,
    val oppgaver: List<Oppgave>,
) {
    override fun toString(): String = "SokOppgaverResponse"
}
