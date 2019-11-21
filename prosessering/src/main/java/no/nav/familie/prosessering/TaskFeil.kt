package no.nav.familie.prosessering

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import no.nav.familie.prosessering.domene.Task

import javax.persistence.Embeddable
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Json struktur for feil som kan oppstå. Dupliserer noen properties for enkelthetsskyld til senere prosessering.
 *
 *
 * Kan kun gjenskapes som json dersom sisteFeil ble lagret som Json i TASK tabell
 * (dvs. i nyere versjoner &gt;=2.4, gamle versjoner lagrer som flat string).
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Embeddable
data class TaskFeil(
        @JsonProperty("taskId")
        val taskId: Long?,
        @JsonProperty("taskName")
        val taskName: String?,
        @JsonProperty("exceptionCauseClass")
        var exceptionCauseClass: String? = null,
        @JsonProperty("exceptionCauseMessage")
        var exceptionCauseMessage: String? = null,
        @JsonProperty("feilmelding")
        var feilmelding: String? = null,
        @JsonProperty("stacktrace")
        var stackTrace: String? = null,
        @JsonProperty("callId")
        val callId: String? = null,
        @JsonProperty("feilkode")
        var feilkode: String? = null
) {

    constructor(taskInfo: Task, feil: Exception?) : this(taskInfo.id,
                                                         taskInfo.taskStepType,
                                                         feil?.cause?.javaClass?.name,
                                                         feil?.cause?.message,
                                                         feil?.message,
                                                         getStacktraceAsString(feil))

    @Throws(IOException::class)
    fun writeValueAsString(): String {
        return objectWriter!!.writeValueAsString(this)
    }

    companion object {

        private var objectWriter: ObjectWriter? = null
        private var objectReader: ObjectReader? = null

        init {
            val om = ObjectMapper()
            objectWriter = om.writerWithDefaultPrettyPrinter()
            objectReader = om.reader()
        }

        private fun getStacktraceAsString(cause: Throwable?): String? {
            if (cause == null) {
                return null
            }
            val sw = StringWriter(4096)
            val pw = PrintWriter(sw)
            cause.printStackTrace(pw)
            pw.flush()
            return sw.toString()
        }
    }

}
