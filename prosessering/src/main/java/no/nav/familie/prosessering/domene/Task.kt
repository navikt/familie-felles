package no.nav.familie.prosessering.domene

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.TaskLogg.Companion.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
import org.slf4j.MDC
import java.io.IOException
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "TASK")
data class Task(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
        @SequenceGenerator(name = "task_seq")
        val id: Long? = null,

        @Column(name = "payload", updatable = false, columnDefinition = "text")
        val payloadId: String,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: Status = Status.UBEHANDLET,

        @Enumerated(EnumType.STRING)
        @Column(name = "avvikstype", updatable = false)
        var avvikstype: Avvikstype? = null,

        @Column(name = "opprettet_tid", nullable = false, updatable = false)
        var opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

        @Column(name = "trigger_tid", nullable = true, updatable = true)
        val triggerTid: LocalDateTime? = null,

        @Column(name = "type", nullable = false, updatable = false)
        val type: String,

        @Convert(converter = PropertiesToStringConverter::class)
        @Column(name = "metadata")
        val metadata: Properties = Properties(),

        // Setter fetch til eager fordi AsyncTask ikke får lastet disse hvis ikke den er prelastet.
        @OneToMany(mappedBy = "task",
                   fetch = FetchType.EAGER,
                   cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH])
        val logg: MutableList<TaskLogg> = ArrayList()
) {

    val callId: String
        get() = this.metadata.getProperty(MDCConstants.MDC_CALL_ID)


    private constructor (type: String, payloadId: String) :
            this(type = type,
                 payloadId = payloadId,
                 metadata = Properties().apply {
                     this[MDCConstants.MDC_CALL_ID] =
                             MDC.get(MDCConstants.MDC_CALL_ID)
                             ?: IdUtils.generateId()
                 })

    private constructor (type: String,
                         payloadId: String,
                         triggerTidspunkt: LocalDateTime) :
            this(type = type,
                 payloadId = payloadId,
                 triggerTid = triggerTidspunkt,
                 metadata = Properties().apply {
                     this[MDCConstants.MDC_CALL_ID] =
                             MDC.get(MDCConstants.MDC_CALL_ID)
                             ?: IdUtils.generateId()
                 })

    @PrePersist
    fun onCreate() {
        this.opprettetTidspunkt = LocalDateTime.now()
        this.logg.add(TaskLogg(this, Loggtype.UBEHANDLET))
    }


    fun avvikshåndter(avvikstype: Avvikstype,
                      årsak: String,
                      endretAv: String): Task {

        this.status = Status.AVVIKSHÅNDTERT
        this.avvikstype = avvikstype
        this.logg.add(TaskLogg(task = this,
                               type = Loggtype.AVVIKSHÅNDTERT,
                               melding = årsak,
                               endretAv = endretAv))
        return this
    }

    fun behandler(): Task {
        this.status = Status.BEHANDLER
        this.logg.add(TaskLogg(this, Loggtype.BEHANDLER))
        return this
    }

    fun klarTilPlukk(endretAv: String): Task {
        this.status = Status.KLAR_TIL_PLUKK
        this.logg.add(TaskLogg(task = this,
                               type = Loggtype.KLAR_TIL_PLUKK,
                               melding = null,
                               endretAv = endretAv))
        return this
    }

    fun plukker(): Task {
        this.status = Status.PLUKKET
        this.logg.add(TaskLogg(this, Loggtype.PLUKKET))
        return this
    }

    fun ferdigstill(): Task {
        this.status = Status.FERDIG
        this.logg.add(TaskLogg(this, Loggtype.FERDIG))
        return this
    }

    fun feilet(feil: TaskFeil, maxAntallFeil: Int): Task {
        try {
            this.logg.add(TaskLogg(task = this,
                                   type = Loggtype.FEILET,
                                   melding = feil.writeValueAsString(),
                                   endretAv = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES))
        } catch (e: IOException) {
            this.logg.add(TaskLogg(this, Loggtype.FEILET))
        }

        val antallFeilendeForsøk = logg
                .filter { it.type == Loggtype.FEILET }
                .size
        if (maxAntallFeil > antallFeilendeForsøk) {
            this.status = Status.KLAR_TIL_PLUKK
        } else {
            this.status = Status.FEILET
        }
        return this
    }

    override fun toString(): String {
        return """Task(id=$id, 
            |payloadId='$payloadId', 
            |status=$status, 
            |avvikstype=$avvikstype, 
            |opprettetTidspunkt=$opprettetTidspunkt, 
            |triggerTid=$triggerTid, 
            |type='$type', 
            |metadata=$metadata)""".trimMargin()
    }


    companion object {

        fun nyTask(type: String, payloadId: String): Task {
            return Task(type, payloadId, LocalDateTime.now())
        }
    }
}
