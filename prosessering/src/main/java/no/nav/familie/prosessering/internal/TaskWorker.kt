package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.log.mdc.MDCConstants.MDC_CALL_ID
import no.nav.familie.prosessering.AsyncTask
import no.nav.familie.prosessering.TaskBeskrivelse
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TaskWorker(private val taskRepository: TaskRepository, taskTyper: List<AsyncTask>) {

    private val tasktypeMap: Map<String, AsyncTask>
    private val maxAntallFeilMap: Map<String, Int>
    private val feiltellereForTasker: Map<String, Counter>

    init {
        val tasksTilTaskBeskrivelse: Map<AsyncTask, TaskBeskrivelse> = taskTyper.associateWith { task ->
            val aClass = AopProxyUtils.ultimateTargetClass(task)
            val annotation = AnnotationUtils.findAnnotation(aClass, TaskBeskrivelse::class.java)
            requireNotNull(annotation) { "annotasjon mangler" }
            annotation
        }
        tasktypeMap = tasksTilTaskBeskrivelse.entries.associate { it.value.tasktype to it.key }
        maxAntallFeilMap = tasksTilTaskBeskrivelse.values.associate { it.tasktype to it.maxAntallFeil }
        feiltellereForTasker = tasksTilTaskBeskrivelse.values.associate {
            it.tasktype to Metrics.counter("mottak.kontantstotte.feilede.tasks",
                                           "status",
                                           it.tasktype,
                                           "beskrivelse",
                                           it.beskrivelse)
        }
    }


    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doTask(taskId: Long?) {
        requireNotNull(taskId, { "taskId kan ikke være null" })
        doActualWork(taskId)
    }

    // For Unit testing
    fun doActualWork(TaskId: Long) {
        val startTidspunkt = System.currentTimeMillis()
        var maxAntallFeil = 0
        var taskDetails = taskRepository.findById(TaskId).orElseThrow()

        initLogContext(taskDetails)
        try {

            secureLog.trace("Behandler task='{}'", taskDetails)
            taskDetails.behandler()
            taskDetails = taskRepository.saveAndFlush(taskDetails)

            // finn tasktype
            val task = finnTask(taskDetails.type)
            maxAntallFeil = finnMaxAntallFeil(taskDetails.type)

            // execute
            task.preCondition(taskDetails)
            task.doTask(taskDetails)
            task.postCondition(taskDetails)
            task.onCompletion(taskDetails)

            taskDetails.ferdigstill()
            secureLog.trace("Ferdigstiller task='{}'", taskDetails)
            taskRepository.saveAndFlush(taskDetails)
            taskRepository.flush()
            secureLog.info("Fullført kjøring av task '{}', kjøretid={} ms",
                           taskDetails,
                           System.currentTimeMillis() - startTidspunkt)
        } catch (e: Exception) {
            taskDetails.feilet(TaskFeil(taskDetails, e), maxAntallFeil)
            // lager metrikker på tasks som har feilet max antall ganger.
            if (taskDetails.status == Status.FEILET) {
                finnFeilteller(taskDetails.type).increment()
            }
            secureLog.warn("Fullført kjøring av task '{}', kjøretid={} ms, feilmelding='{}'",
                           taskDetails,
                           System.currentTimeMillis() - startTidspunkt,
                           e)
            taskRepository.save(taskDetails)
        } finally {
            clearLogContext()
        }
    }

    private fun clearLogContext() {
        LOG_CONTEXT.clear()
        MDC.remove(MDC_CALL_ID)
    }

    private fun initLogContext(taskDetails: Task) {
        MDC.put(MDC_CALL_ID, taskDetails.callId)
        LOG_CONTEXT.add("task", taskDetails.type)
    }

    private fun finnTask(taskType: String): AsyncTask {
        return tasktypeMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnFeilteller(taskType: String): Counter {
        return feiltellereForTasker[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnMaxAntallFeil(taskType: String): Int {
        return maxAntallFeilMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    companion object {

        private val LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess")
        private val secureLog = LoggerFactory.getLogger("secureLogger")
    }
}
