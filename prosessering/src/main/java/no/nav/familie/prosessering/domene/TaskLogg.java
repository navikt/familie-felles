package no.nav.familie.prosessering.domene;

import javax.persistence.*;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

@Entity
@Table(name = "TASK_LOGG")
public class TaskLogg {

    public static final String BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_logg_seq")
    @SequenceGenerator(name = "task_logg_seq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "endret_av", nullable = false, updatable = false)
    private String endretAv;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private LoggType type;

    @Column(name = "node")
    private String node;

    @Column(name = "melding", updatable = false, columnDefinition = "text")
    private String melding;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    TaskLogg() {
        String hostname = "node1";
        this.node = hostname;
    }

    public TaskLogg(Task task, LoggType type) {
        this();
        this.endretAv = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES;
        this.task = task;
        this.type = type;
    }

    public TaskLogg(Task task, LoggType type, String melding, String endretAv) {
        this(task, type);
        this.melding = melding;
        this.endretAv = endretAv;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public String getMelding() {
        return melding;
    }

    public LoggType getType() {
        return type;
    }

    public String getNode() {
        return node;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    @PrePersist
    protected void onCreate() {
        this.opprettetTidspunkt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "TaskLogg{" +
            "id=" + id +
            ", type=" + type +
            ", node='" + node + '\'' +
            ", opprettetTidspunkt=" + opprettetTidspunkt +
            '}';
    }
}
