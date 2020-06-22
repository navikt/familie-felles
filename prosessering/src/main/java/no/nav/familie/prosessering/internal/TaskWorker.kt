package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.log.mdc.MDCConstants.MDC_CALL_ID
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TaskWorker(private val taskRepository: TaskRepository, taskStepTyper: List<AsyncTaskStep>) {

    private val taskStepMap: Map<String, AsyncTaskStep>
    private val maxAntallFeilMap: Map<String, Int>
    private val feiltellereForTaskSteps: Map<String, Counter>

    init {
        val tasksTilTaskStepBeskrivelse: Map<AsyncTaskStep, TaskStepBeskrivelse> = taskStepTyper.associateWith { task ->
            val aClass = AopProxyUtils.ultimateTargetClass(task)
            val annotation = AnnotationUtils.findAnnotation(aClass, TaskStepBeskrivelse::class.java)
            requireNotNull(annotation) { "annotasjon mangler" }
            annotation
        }
        taskStepMap = tasksTilTaskStepBeskrivelse.entries.associate { it.value.taskStepType to it.key }
        maxAntallFeilMap = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.maxAntallFeil }
        feiltellereForTaskSteps = tasksTilTaskStepBeskrivelse.values.associate {
            it.taskStepType to Metrics.counter("mottak.feilede.tasks",
                                               "status",
                                               it.taskStepType,
                                               "beskrivelse",
                                               it.beskrivelse)
        }
    }


    // For Unit testing
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doWork(taskId: Long) {
        doActualWork(taskId)
    }

    fun doActualWork(taskId: Long) {
        val startTidspunkt = System.currentTimeMillis()
        var maxAntallFeil = 0
        var task = taskRepository.findByIdOrNull(taskId) ?: error("Kunne ikke finne task med id $taskId")

        initLogContext(task)
        try {

            secureLog.trace("Behandler task='{}'", task)
            task.behandler()
            task = taskRepository.saveAndFlush(task)

            // finn tasktype
            val taskStep = finnTaskStep(task.taskStepType)
            maxAntallFeil = finnMaxAntallFeil(task.taskStepType)

            // execute
            taskStep.preCondition(task)
            taskStep.doTask(task)
            taskStep.postCondition(task)
            taskStep.onCompletion(task)

            task.ferdigstill()
            secureLog.trace("Ferdigstiller task='{}'", task)
            taskRepository.saveAndFlush(task)
            taskRepository.flush()
            secureLog.info("Fullført kjøring av task '{}', kjøretid={} ms",
                           task,
                           System.currentTimeMillis() - startTidspunkt)
        } catch (e: Exception) {
            task.feilet(TaskFeil(task, e), maxAntallFeil)
            // lager metrikker på tasks som har feilet max antall ganger.
            if (task.status == Status.FEILET) {
                finnFeilteller(task.taskStepType).increment()
            }
            secureLog.warn("Fullført kjøring av task '{}', kjøretid={} ms, feilmelding='{}'",
                           task,
                           System.currentTimeMillis() - startTidspunkt,
                           e)
            taskRepository.save(task)
            secureLog.info("Feilhåndtering lagret ok {}", task)
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
        LOG_CONTEXT.add("task", taskDetails.taskStepType)
    }

    private fun finnTaskStep(taskType: String): AsyncTaskStep {
        return taskStepMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnFeilteller(taskType: String): Counter {
        return feiltellereForTaskSteps[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnMaxAntallFeil(taskType: String): Int {
        return maxAntallFeilMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    companion object {

        private val LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess")
        private val secureLog = LoggerFactory.getLogger("secureLogger")
    }
}
