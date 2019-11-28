package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.domene.TaskLogg.Companion.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private const val CRON_DAILY_8AM = "0 0 8 1/1 * ?"
private const val CRON_DAILY_9AM = "0 0 9 1/1 * ?"

@Service
class ScheduledTaskService(private val taskRepository: TaskRepository) {

    @Scheduled(cron = CRON_DAILY_8AM)
    @Transactional
    fun retryFeilendeTask() {
        val tasks = taskRepository.finnAlleFeiledeTasks()

        tasks.forEach { taskRepository.save(it.klarTilPlukk(BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES)) }
    }

    @Scheduled(cron = CRON_DAILY_9AM)
    @Transactional
    fun slettTasksKlarForSletting() {
        val klarForSletting = taskRepository.finnTasksKlarForSletting(LocalDateTime.now().minusWeeks(2))
        klarForSletting.forEach {
            logger.info("Task klar for sletting. {} {} {} {}", it.id, it.callId, it.triggerTid, it.status)
            taskRepository.delete(it)
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ScheduledTaskService::class.java)
    }
}
