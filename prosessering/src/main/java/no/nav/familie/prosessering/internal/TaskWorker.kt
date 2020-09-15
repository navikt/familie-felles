package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TaskWorker(private val taskRepository: TaskRepository, taskStepTyper: List<AsyncTaskStep>) {

    private val taskStepMap: Map<String, AsyncTaskStep>

    private val maxAntallFeilMap: Map<String, Int>
    private val triggerTidVedFeilMap: Map<String, Long>
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
        triggerTidVedFeilMap = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.triggerTidVedFeilISekunder }
        feiltellereForTaskSteps = tasksTilTaskStepBeskrivelse.values.associate {
            it.taskStepType to Metrics.counter("mottak.feilede.tasks",
                                               "status",
                                               it.taskStepType,
                                               "beskrivelse",
                                               it.beskrivelse)
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doActualWork(taskId: Long) {

        val taskOptional = taskRepository.findById(taskId)

        if (taskOptional.isEmpty) {
            return
        }

        val task = taskOptional.get()
        if (task.status != Status.PLUKKET) {
            return // en annen pod har startet behandling
        }

        task.behandler()

        // finn tasktype
        val taskStep = finnTaskStep(task.taskStepType)

        // execute
        taskStep.preCondition(task)
        taskStep.doTask(task)
        taskStep.postCondition(task)
        taskStep.onCompletion(task)

        task.ferdigstill()
        secureLog.trace("Ferdigstiller task='{}'", task)

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doFeilhåndtering(task: Task, e: Exception) {
        val maxAntallFeil = finnMaxAntallFeil(task.taskStepType)
        secureLog.trace("Behandler task='{}'", task)

        task.feilet(TaskFeil(task, e), maxAntallFeil)
        // lager metrikker på tasks som har feilet max antall ganger.
        if (task.status == Status.FEILET) {
            finnFeilteller(task.taskStepType).increment()
            log.error("Task ${task.id} av type ${task.taskStepType} har feilet. Sjekk familie-prosessering for detaljer")
        }
        task.triggerTid = task.triggerTid?.plusSeconds(finnTriggerTidVedFeil(task.taskStepType))
        taskRepository.save(task)
        secureLog.info("Feilhåndtering lagret ok {}", task)

    }


    private fun finnTriggerTidVedFeil(taskType: String): Long {
        return triggerTidVedFeilMap[taskType] ?: 0
    }

    private fun finnFeilteller(taskType: String): Counter {
        return feiltellereForTaskSteps[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnMaxAntallFeil(taskType: String): Int {
        return maxAntallFeilMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnTaskStep(taskType: String): AsyncTaskStep {
        return taskStepMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markerPlukket(id: Long) {
        val taskOptional = taskRepository.findById(id)

        if (taskOptional.isEmpty) {
            return
        }

        val task = taskOptional.get()
        if (task.status.kanPlukkes()) {
            task.plukker()
        }
    }

    companion object {
        private val secureLog = LoggerFactory.getLogger("secureLogger")
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
