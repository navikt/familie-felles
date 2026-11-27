package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegistrerKommentar(
    val tekst: String? = null,
    val automatiskGenerert: Boolean? = null,
) {
    override fun toString(): String = "RegistrerKommentar"
}
