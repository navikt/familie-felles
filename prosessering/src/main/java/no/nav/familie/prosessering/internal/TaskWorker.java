package no.nav.familie.prosessering.internal;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.familie.prosessering.AsyncTask;
import no.nav.familie.prosessering.TaskBeskrivelse;
import no.nav.familie.prosessering.TaskFeil;
import no.nav.familie.prosessering.domene.Status;
import no.nav.familie.prosessering.domene.Task;
import no.nav.familie.prosessering.domene.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static no.nav.familie.log.mdc.MDCConstants.MDC_CALL_ID;

@Service
class TaskWorker {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger log = LoggerFactory.getLogger(TaskWorker.class);
    private static final Logger secureLog = LoggerFactory.getLogger("secureLogger");
    private final TaskRepository taskRepository;
    private Map<String, AsyncTask> tasktypeMap = new HashMap<>();
    private Map<String, Integer> maxAntallFeilMap = new HashMap<>();
    private final HashMap<String, Counter> feiledeTasks = new HashMap<>();

    @Autowired
    public TaskWorker(TaskRepository taskRepository, List<AsyncTask> taskTyper) {
        this.taskRepository = taskRepository;
        taskTyper.forEach(this::kategoriserTask);
    }

    private void kategoriserTask(AsyncTask task) {
        final Class<?> aClass = AopProxyUtils.ultimateTargetClass(task);
        final var annotation = AnnotationUtils.findAnnotation(aClass, TaskBeskrivelse.class);
        Objects.requireNonNull(annotation, "annotasjon mangler");
        tasktypeMap.put(annotation.taskType(), task);
        maxAntallFeilMap.put(annotation.taskType(), annotation.maxAntallFeil());

        feiledeTasks.put(annotation.taskType(), Metrics.counter("mottak.kontantstotte.feilede.tasks", "status", annotation.taskType(), "beskrivelse", annotation.beskrivelse()));
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doTask(Long henvendelseId) {
        Objects.requireNonNull(henvendelseId, "id kan ikke være null");
        doActualWork(henvendelseId);
    }

    // For testing
    void doActualWork(Long henvendelseId) {
        final var startTidspunkt = System.currentTimeMillis();
        Integer maxAntallFeil = 0;
        var taskDetails = taskRepository.findById(henvendelseId).orElseThrow();

        initLogContext(taskDetails);
        try {

            secureLog.trace("Behandler task='{}'", taskDetails);
            taskDetails.behandler();
            taskDetails = taskRepository.saveAndFlush(taskDetails);

            // finn tasktype
            AsyncTask task = finnTask(taskDetails.getType());
            maxAntallFeil = finnMaxAntallFeil(taskDetails.getType());

            // execute
            task.preCondition(taskDetails);
            task.doTask(taskDetails);
            task.postCondition(taskDetails);
            task.onCompletion(taskDetails);

            taskDetails.ferdigstill();
            secureLog.trace("Ferdigstiller task='{}'", taskDetails);
            taskRepository.saveAndFlush(taskDetails);
            taskRepository.flush();
            secureLog.info("Fullført kjøring av task '{}', kjøretid={} ms", taskDetails, (System.currentTimeMillis() - startTidspunkt));
        } catch (Exception e) {
            taskDetails.feilet(new TaskFeil(taskDetails, e), maxAntallFeil);
            // lager metrikker på tasks som har feilet max antall ganger.
            if (taskDetails.getStatus() == Status.FEILET) {
                feiledeTasks.get(taskDetails.getType()).increment();
            }
            secureLog.warn("Fullført kjøring av task '{}', kjøretid={} ms, feilmelding='{}'", taskDetails, (System.currentTimeMillis() - startTidspunkt), e);
            taskRepository.save(taskDetails);
        } finally {
            clearLogContext();
        }
    }

    private void clearLogContext() {
        LOG_CONTEXT.clear();
        MDC.remove(MDC_CALL_ID);
    }

    private void initLogContext(Task taskDetails) {
        MDC.put(MDC_CALL_ID, taskDetails.getCallId());
        LOG_CONTEXT.add("task", taskDetails.getType());
    }

    private AsyncTask finnTask(String taskType) {
        if (!tasktypeMap.containsKey(taskType)) {
            throw new IllegalArgumentException("Ukjent tasktype " + taskType);
        }
        return tasktypeMap.get(taskType);
    }

    private Integer finnMaxAntallFeil(String taskType) {
        if (!maxAntallFeilMap.containsKey(taskType)) {
            throw new IllegalArgumentException("Ukjent tasktype " + taskType);
        }
        return maxAntallFeilMap.get(taskType);
    }
}
