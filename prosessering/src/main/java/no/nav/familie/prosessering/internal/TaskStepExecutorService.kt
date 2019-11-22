package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskExecutor
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.function.Consumer
import kotlin.math.min

@Service
class TaskStepExecutorService(@Value("\${prosessering.maxAntall:10}") private val maxAntall: Int,
                              @Value("\${prosessering.minCapacity:2}") private val minCapacity: Int,
                              private val worker: TaskWorker,
                              @Qualifier("taskExecutor") private val taskExecutor: TaskExecutor,
                              private val taskRepository: TaskRepository) {

    @Scheduled(fixedDelay = POLLING_DELAY)
    @Transactional
    fun pollAndExecute() {
        log.debug("Poller etter nye tasks")
        val pollingSize = calculatePollingSize(maxAntall)

        if (pollingSize > minCapacity) {
            val tasks = taskRepository.finnAlleTasksKlareForProsessering(PageRequest.of(0, pollingSize))
            log.trace("Pollet {} tasks med max {}", tasks.size, maxAntall)

            tasks.forEach(Consumer<Task> { this.executeWork(it) })
        } else {
            log.trace("Pollet ingen tasks siden kapasiteten var {} < {}", pollingSize, minCapacity)
        }
        log.trace("Ferdig med polling, venter {} ms til neste kjøring.", POLLING_DELAY)
    }

    private fun calculatePollingSize(maxAntall: Int): Int {
        val remainingCapacity = (taskExecutor as ThreadPoolTaskExecutor).threadPoolExecutor.queue.remainingCapacity()
        val pollingSize = min(remainingCapacity, maxAntall)
        log.trace("Ledig kapasitet i kø {}, poller etter {}", remainingCapacity, pollingSize)
        return pollingSize
    }

    private fun executeWork(task: Task) {
        task.plukker()
        taskRepository.saveAndFlush(task)
        worker.doTaskStep(task.id!!)
    }

    companion object {
        const val POLLING_DELAY = 30000L
        private val log = LoggerFactory.getLogger(TaskStepExecutorService::class.java)
    }
}
