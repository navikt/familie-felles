package no.nav.familie.prosessering.internal

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class TaskStepRunner(private val taskWorker: TaskWorker) {

    @Async("taskExecutor")
    fun doTaskStep(taskId: Long?) {
        requireNotNull(taskId, { "taskId kan ikke være null" })
        taskWorker.doWork(taskId)
    }
}
