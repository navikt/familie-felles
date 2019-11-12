package no.nav.familie.prosessering.domene;

import no.nav.familie.log.IdUtils;
import no.nav.familie.log.mdc.MDCConstants;
import no.nav.familie.prosessering.TaskFeil;
import org.slf4j.MDC;

import javax.persistence.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static no.nav.familie.prosessering.domene.TaskLogg.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES;

@Entity
@Table(name = "TASK")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
    @SequenceGenerator(name = "task_seq")
    private Long id;

    @Column(name = "payload", updatable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.UBEHANDLET;

    @Enumerated(EnumType.STRING)
    @Column(name = "avvikstype", updatable = false)
    private Avvikstype avvikstype;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    @Column(name = "trigger_tid", nullable = true, updatable = true)
    private LocalDateTime triggerTid;

    @Column(name = "type", nullable = false, updatable = false)
    private String type;

    @Convert(converter = PropertiesToStringConverter.class)
    @Column(name = "metadata")
    private Properties metadata = new Properties();

    @Version
    private Long versjon;

    // Setter fetch til eager fordi asynctask ikke får lastet disse hvis ikke den er prelastet.
    @OneToMany(mappedBy = "task", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<TaskLogg> logg = new ArrayList<>();

    Task() {
    }

    public static Task nyTaskMedStartFremITid(String type, String payload, LocalDateTime triggerTidspunkt) {
        return new Task(type, payload, triggerTidspunkt);
    }

    public static Task nyTask(String type, String payload) {
       return new Task(type, payload, LocalDateTime.now());
    }

    private Task(String type, String payload) {
        this.type = type;
        this.payload = payload;
        this.metadata.put(MDCConstants.MDC_CALL_ID, Objects.requireNonNullElseGet(MDC.get(MDCConstants.MDC_CALL_ID), IdUtils::generateId));
    }

    private Task(String type, String payload, LocalDateTime triggerTidspunkt) {
        this.type = type;
        this.payload = payload;
        this.triggerTid = triggerTidspunkt;
        this.metadata.put(MDCConstants.MDC_CALL_ID, Objects.requireNonNullElseGet(MDC.get(MDCConstants.MDC_CALL_ID), IdUtils::generateId));
    }

    @PrePersist
    protected void onCreate() {
        this.opprettetTidspunkt = LocalDateTime.now();
        this.logg.add(new TaskLogg(this, LoggType.UBEHANDLET));
    }

    public String getPayload() {
        return payload;
    }

    public Task setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    public Task avvikshåndter(Avvikstype avviksType, String årsak, String endretAv) {
        this.status = Status.AVVIKS_HÅNDTERT;
        this.avvikstype = avviksType;
        this.logg.add(new TaskLogg(this, LoggType.AVVIKS_HÅNDTERT, årsak, endretAv));
        return this;
    }

    public Task behandler() {
        this.status = Status.BEHANDLER;
        this.logg.add(new TaskLogg(this, LoggType.BEHANDLER));
        return this;
    }

    public Task klarTilPlukk(String endretAv) {
        this.status = Status.KLAR_TIL_PLUKK;
        this.logg.add(new TaskLogg(this, LoggType.KLAR_TIL_PLUKK, null, endretAv));
        return this;
    }

    public Task plukker() {
        this.status = Status.PLUKKET;
        this.logg.add(new TaskLogg(this, LoggType.PLUKKET));
        return this;
    }

    public Long getId() {
        return id;
    }

    public Task ferdigstill() {
        this.status = Status.FERDIG;
        this.logg.add(new TaskLogg(this, LoggType.FERDIG));
        return this;
    }

    public List<TaskLogg> getLogg() {
        return logg;
    }

    public LocalDateTime getTriggerTid() {
        return triggerTid;
    }

    public void setTriggerTid(LocalDateTime triggerTidspunkt) {
        this.triggerTid = triggerTidspunkt;
    }

    public Status getStatus() {
        return status;
    }

    public Avvikstype getAvvikstype() {
        return avvikstype;
    }

    public LocalDateTime getOpprettetTidspunkt () {
        return opprettetTidspunkt;
    }

    public String getCallId() {
        return this.metadata.getProperty(MDCConstants.MDC_CALL_ID);
    }

    public Properties getMetadata() {
        return metadata;
    }

    public Task feilet(TaskFeil feil, int maxAntallFeil) {
        try {
            this.logg.add(new TaskLogg(this, LoggType.FEILET, feil.writeValueAsString(), BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES));
        } catch (IOException e) {
            this.logg.add(new TaskLogg(this, LoggType.FEILET));
        }
        final var antallFeilendeForsøk = logg.stream().filter(it -> it.getType().equals(LoggType.FEILET)).count();
        if (maxAntallFeil > antallFeilendeForsøk) {
            this.status = Status.KLAR_TIL_PLUKK;
        } else {
            this.status = Status.FEILET;
        }
        return this;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", status=" + status +
                ", opprettetTidspunkt=" + opprettetTidspunkt +
                ", triggertid=" + triggerTid +
                ", versjon=" + versjon +
                ", type=" + type +
                '}';
    }
}
