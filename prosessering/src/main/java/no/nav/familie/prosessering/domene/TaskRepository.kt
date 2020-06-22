package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*
import javax.persistence.LockModeType

@Repository
interface TaskRepository : JpaRepository<Task, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    override fun findById(id: Long): Optional<Task>

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(task: Task): Task

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.status IN ('KLAR_TIL_PLUKK', 'UBEHANDLET')  " +
           "AND  (t.triggerTid < CURRENT_TIMESTAMP OR t.triggerTid IS NULL) ORDER BY t.opprettetTidspunkt DESC")
    fun finnAlleTasksKlareForProsessering(page: Pageable): List<Task>

    @Query("SELECT t FROM Task t WHERE t.status IN ('KLAR_TIL_PLUKK', 'UBEHANDLET')  " +
           "AND  (t.triggerTid < CURRENT_TIMESTAMP OR t.triggerTid IS NULL) ORDER BY t.opprettetTidspunkt DESC")
    fun finnAlleTasksKlareForProsesseringUtenLock(page: Pageable): List<Task>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.status IN ('FEILET')")
    fun finnAlleFeiledeTasks(): List<Task>

    @Query("SELECT t FROM Task t WHERE t.status IN (:status) ORDER BY t.opprettetTidspunkt DESC")
    fun finnTasksTilFrontend(status: List<Status>, page: Pageable): List<Task>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.status = 'FERDIG' AND t.triggerTid < :eldreEnnDato")
    fun finnTasksKlarForSletting(eldreEnnDato: LocalDateTime): List<Task>
}
