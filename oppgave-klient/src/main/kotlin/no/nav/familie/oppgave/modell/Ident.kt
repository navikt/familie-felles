package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ident(
    val ident: String? = null,
    val gruppe: Gruppe? = null,
) {
    override fun toString(): String = "Ident"
}
