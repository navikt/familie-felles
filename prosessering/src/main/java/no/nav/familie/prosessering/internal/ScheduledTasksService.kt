package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import no.nav.familie.prosessering.domene.TaskLogg.Companion.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES

@Service
class ScheduledTasksService(private val taskRepository: TaskRepository) {

    @Scheduled(cron = "0 0 8 1/1 * ?")
    @Transactional
    fun retryFeilendeTask() {
        val tasks = taskRepository.finnAlleFeiledeTasks()

        tasks.forEach { taskRepository.save(it.klarTilPlukk(BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES)) }
    }
}
