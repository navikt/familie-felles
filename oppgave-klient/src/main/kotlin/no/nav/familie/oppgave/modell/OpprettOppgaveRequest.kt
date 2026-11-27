package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettOppgaveRequest(
    @field:NotNull
    val aktivDato: LocalDate,
    @field:NotBlank
    @field:Size(max = 255)
    val oppgavetype: String,
    @field:NotNull
    val prioritet: Prioritet,
    @field:NotBlank
    @field:Size(max = 255)
    val tema: String,
    /**
     * Konsumenten må lage og lagre én UUID per logisk oppretting og gjenbruke den ved retry og etter omstart.
     * UUID-en må ikke utledes fra personopplysninger eller callId.
     */
    @field:NotNull
    val uuid: UUID,
    @field:Size(min = 13, max = 13)
    val aktoerId: String? = null,
    @field:Size(min = 11, max = 13)
    val personident: String? = null,
    @field:Size(max = 9)
    val orgnr: String? = null,
    @field:Size(max = 40)
    val bnr: String? = null,
    @field:Size(max = 40)
    val samhandlernr: String? = null,
    @field:Size(min = 4, max = 4)
    val tildeltEnhetsnr: String? = null,
    @field:Size(min = 4, max = 4)
    val opprettetAvEnhetsnr: String? = null,
    @field:Size(max = 40)
    val journalpostId: String? = null,
    @field:Size(max = 40)
    val journalpostkilde: String? = null,
    @field:Size(max = 7)
    val tilordnetRessurs: String? = null,
    @field:Size(max = 40)
    val behandlesAvApplikasjon: String? = null,
    @field:Size(max = 40)
    val saksreferanse: String? = null,
    val beskrivelse: String? = null,
    @field:Size(max = 255)
    val temagruppe: String? = null,
    val mappeId: Long? = null,
    val metadata: Map<MetadataKey, String>? = null,
    @field:Size(max = 255)
    val behandlingstema: String? = null,
    @field:Size(max = 255)
    val behandlingstype: String? = null,
    val fristFerdigstillelse: LocalDate? = null,
) {
    override fun toString(): String = "OpprettOppgaveRequest"
}
