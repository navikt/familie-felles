package no.nav.familie.prosessering.domene;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Task> findById(Long id);

    @SuppressWarnings("unchecked")
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    Task save(Task task);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        value = "SELECT t FROM Task t WHERE t.status IN ('KLAR_TIL_PLUKK', 'UBEHANDLET')  AND  (t.triggerTid < CURRENT_TIMESTAMP OR t.triggerTid IS NULL) ORDER BY t.opprettetTidspunkt DESC")
    List<Task> finnAlleTasksKlareForProsessering(Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.status IN ('FEILET')")
    List<Task> finnAlleFeiledeTasks();

    @Query("SELECT t FROM Task t WHERE t.status IN :status")
    List<Task> finnTasksTilFrontend(Status status);
}
