package no.nav.familie.prosessering.rest

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Status
import java.time.LocalDateTime
import java.util.*

// Legger opp for fremtidlig mulighet for å legge inn metadata som sidnummer, etc
data class PaginableResponse<T>(val tasks: List<T>)

data class TaskDto(val id: Long,
                   val status: Status = Status.UBEHANDLET,
                   val avvikstype: Avvikstype?,
                   val opprettetTidspunkt: LocalDateTime,
                   val triggerTid: LocalDateTime?,
                   val taskStepType: String,
                   val metadata: Properties,
                   val payload: String,
                   val antallLogger: Long,
                   val sistKjørt: LocalDateTime?){
    val callId: String
        get() = this.metadata.getProperty(MDCConstants.MDC_CALL_ID)
}

data class TaskloggDto(val id: Long,
                       val endretAv: String?,
                       val type: Loggtype,
                       val node: String,
                       val melding: String?,
                       val opprettetTidspunkt: LocalDateTime)
