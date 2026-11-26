package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SokOppgaverResponse(
    val antallTreffTotalt: Long? = null,
    val oppgaver: List<Oppgave>? = null,
)
