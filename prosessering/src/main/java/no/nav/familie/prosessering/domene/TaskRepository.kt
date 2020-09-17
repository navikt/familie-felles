package no.nav.familie.prosessering.domene

import no.nav.familie.prosessering.rest.TaskDto
import no.nav.familie.prosessering.rest.TaskloggDto
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
    fun finnTasksMedStatus(status: List<Status>, page: Pageable): List<Task>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.status = 'FERDIG' AND t.triggerTid < :eldreEnnDato")
    fun finnTasksKlarForSletting(eldreEnnDato: LocalDateTime): List<Task>


    @Query("""select new no.nav.familie.prosessering.rest.TaskDto(t.id, t.status, t.avvikstype, t.opprettetTidspunkt,
        t.triggerTid, t.taskStepType, t.metadata, t.payload, 
        (select count(tl.id) from TaskLogg tl where tl.task.id = t.id), 
        (select max(tl.opprettetTidspunkt) from TaskLogg tl where tl.task.id = t.id))
        from Task t WHERE t.status IN (:status) ORDER BY t.opprettetTidspunkt DESC""")
    fun finnTasksDtoTilFrontend(status: List<Status>, page: Pageable): List<TaskDto>

    @Query("""select new no.nav.familie.prosessering.rest.TaskloggDto(t.id,t.endretAv,t.type,t.node,t.melding,
        t.opprettetTidspunkt) from TaskLogg t WHERE t.task.id = :id ORDER BY t.opprettetTidspunkt DESC""")
    fun finnTaskloggTilFrontend(id: Long): List<TaskloggDto>

}
