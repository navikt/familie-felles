package no.nav.familie.prosessering;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nav.familie.prosessering.domene.Task;

import javax.persistence.Embeddable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Json struktur for feil som kan oppstå. Dupliserer noen properties for enkelthets skyld til senere prosessering.
 * <p>
 * Kan kun gjenskapes som json dersom sisteFeil ble lagret som Json i TASK tabell
 * (dvs. i nyere versjoner &gt;=2.4, gamle versjoner lagrer som flat string).
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Embeddable
public class TaskFeil {

    private static ObjectWriter objectWriter;
    private static ObjectReader objectReader;

    static {
        ObjectMapper om = new ObjectMapper();
        objectWriter = om.writerWithDefaultPrettyPrinter();
        objectReader = om.reader();
    }

    @JsonProperty("exceptionCauseClass")
    private String exceptionCauseClass;

    @JsonProperty("exceptionCauseMessage")
    private String exceptionCauseMessage;

    @JsonProperty("taskName")
    private String taskName;

    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("callId")
    private String callId;

    @JsonProperty("feilkode")
    private String feilkode;

    @JsonProperty("feilmelding")
    private String feilmelding;

    @JsonProperty("stacktrace")
    private String stackTrace;

    public TaskFeil() {
        // default ctor for proxy
    }

    public TaskFeil(Task taskInfo, Exception feil) {
        if (feil != null) {
            Throwable cause = getCause(feil);

            if (cause != null) {
                // bruker her unwrapped cause hvis finnes
                this.exceptionCauseClass = cause.getClass().getName();
                this.exceptionCauseMessage = cause.getMessage();
                this.feilmelding = cause.getMessage();
            }

            // her brukes original exception (ikke unwrapped) slik at vi får med hele historikken hvor eksakt dette inntraff
            this.stackTrace = getStacktraceAsString(feil);// bruker original exception uansett (inkludert wrapping exceptions)
        }

        this.taskName = taskInfo.getType();
        this.taskId = taskInfo.getId() == null ? null : taskInfo.getId().toString();
    }

    public static String readFrom(String str) throws IOException {
        return objectReader.readValue(str);
    }

    public String getExceptionCauseClass() {
        return exceptionCauseClass;
    }

    public String getExceptionCauseMessage() {
        return exceptionCauseMessage;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getCallId() {
        return callId;
    }

    public String getFeilkode() {
        return feilkode;
    }

    public void setFeilkode(String feilkode) {
        this.feilkode = feilkode;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    private String getStacktraceAsString(Throwable cause) {
        if (cause == null) {
            return null;
        }
        StringWriter sw = new StringWriter(4096);
        PrintWriter pw = new PrintWriter(sw);
        cause.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private Throwable getCause(Exception feil) {
        if (feil == null) {
            return null;
        }
        Throwable cause = feil.getCause();
        return cause;
    }

    public String writeValueAsString() throws IOException {
        return objectWriter.writeValueAsString(this);
    }

}
