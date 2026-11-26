package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegistrerKommentarJson(
    val tekst: String? = null,
    val automatiskGenerert: Boolean? = null,
)
