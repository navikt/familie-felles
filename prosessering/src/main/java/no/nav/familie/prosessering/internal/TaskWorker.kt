package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.log.mdc.MDCConstants.MDC_CALL_ID
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
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
class TaskWorker(private val taskRepository: TaskRepository, taskStepTyper: List<AsyncTaskStep>) {

    private val taskStepMap: Map<String, AsyncTaskStep>

    init {
        val tasksTilTaskStepBeskrivelse: Map<AsyncTaskStep, TaskStepBeskrivelse> = taskStepTyper.associateWith { task ->
            val aClass = AopProxyUtils.ultimateTargetClass(task)
            val annotation = AnnotationUtils.findAnnotation(aClass, TaskStepBeskrivelse::class.java)
            requireNotNull(annotation) { "annotasjon mangler" }
            annotation
        }
        taskStepMap = tasksTilTaskStepBeskrivelse.entries.associate { it.value.taskStepType to it.key }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doActualWork(taskId: Long) {

        var task = taskRepository.findById(taskId).orElseThrow()
        task.behandler()
        task = taskRepository.saveAndFlush(task)

        // finn tasktype
        val taskStep = finnTaskStep(task.taskStepType)

        // execute
        execute(taskStep, task)

        task.ferdigstill()
        secureLog.trace("Ferdigstiller task='{}'", task)
        taskRepository.saveAndFlush(task)
        taskRepository.flush()
    }

    fun execute(taskStep: AsyncTaskStep, task: Task) {
        taskStep.preCondition(task)
        taskStep.doTask(task)
        taskStep.postCondition(task)
        taskStep.onCompletion(task)
    }

    private fun finnTaskStep(taskType: String): AsyncTaskStep {
        return taskStepMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    companion object {
        private val secureLog = LoggerFactory.getLogger("secureLogger")
    }
}
