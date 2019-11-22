package no.nav.familie.prosessering.domene

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "TASK_LOGG")
data class TaskLogg(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_logg_seq")
        @SequenceGenerator(name = "task_logg_seq")
        val id: Long? = null,

        @ManyToOne
        @JoinColumn(name = "task_id")
        val task: Task,

        @Column(name = "endret_av", nullable = false, updatable = false)
        var endretAv: String? = null,

        @Enumerated(EnumType.STRING)
        @Column(name = "type", nullable = false, updatable = false)
        val type: Loggtype,

        @Column(name = "node")
        val node: String = "node1",

        @Column(name = "melding", updatable = false, columnDefinition = "text")
        val melding: String? = null,

        @Column(name = "opprettet_tid", nullable = false, updatable = false)
        private var opprettetTidspunkt: LocalDateTime? = LocalDateTime.now()

) {

    @PrePersist
    fun onCreate() {
        this.opprettetTidspunkt = LocalDateTime.now()
    }

    override fun toString(): String {
        return """TaskLogg(id=$id, 
            |endretAv=$endretAv, 
            |type=$type, 
            |node='$node', 
            |melding=$melding, 
            |opprettetTidspunkt=$opprettetTidspunkt)""".trimMargin()
    }

    constructor(task: Task, type: Loggtype) : this(endretAv = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES,
                                                   task = task,
                                                   type = type)


    companion object {

        const val BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL"
    }
}
