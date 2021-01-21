package no.nav.familie.prosessering.rest

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class RestTaskService(private val taskRepository: TaskRepository) {

    fun hentTasks(statuses: List<Status>, saksbehandlerId: String, page: Int): Ressurs<List<Task>> {
        logger.info("$saksbehandlerId henter tasker med status $statuses")

        return Result.runCatching {
            taskRepository.finnTasksMedStatus(statuses, PageRequest.of(page, TASK_LIMIT))
        }
                .fold(
                        onSuccess = { Ressurs.success(data = it) },
                        onFailure = { e ->
                            logger.error("Henting av tasker feilet", e)
                            Ressurs.failure(errorMessage = "Henting av tasker med status '$statuses', feilet.", error = e)
                        }
                )
    }

    fun hentTasks2(statuses: List<Status>, saksbehandlerId: String, page: Int): Ressurs<PaginableResponse<TaskDto>> {
        logger.info("$saksbehandlerId henter tasker med status $statuses")

        return Result.runCatching {
            PaginableResponse(taskRepository.finnTasksDtoTilFrontend(statuses, PageRequest.of(page, TASK_LIMIT)))
        }
                .fold(
                        onSuccess = { Ressurs.success(data = it) },
                        onFailure = { e ->
                            logger.error("Henting av tasker feilet", e)
                            Ressurs.failure(errorMessage = "Henting av tasker med status '$statuses', feilet.", error = e)
                        }
                )
    }

    fun hentTaskLogg(id: Long, saksbehandlerId: String): Ressurs<List<TaskloggDto>> {
        logger.info("$saksbehandlerId henter tasklogg til task=$id")

        return Result.runCatching {
            taskRepository.finnTaskloggTilFrontend(id)
        }
                .fold(
                        onSuccess = { Ressurs.success(data = it) },
                        onFailure = { e ->
                            logger.error("Henting av tasker feilet", e)
                            Ressurs.failure(errorMessage = "Henting av tasklogg feilet.", error = e)
                        }
                )
    }

    @Transactional
    fun rekjørTask(taskId: Long, saksbehandlerId: String): Ressurs<String> {
        val task: Optional<Task> = taskRepository.findById(taskId)

        return when (task.isPresent) {
            true -> {
                taskRepository.save(task.get().copy(triggerTid = LocalDateTime.now()).klarTilPlukk(saksbehandlerId))
                logger.info("$saksbehandlerId rekjører task $taskId")

                Ressurs.success(data = "")
            }

            false -> Ressurs.failure("Fant ikke task med task id $taskId")
        }
    }

    @Transactional
    fun rekjørTasks(status: Status, saksbehandlerId: String): Ressurs<String> {
        logger.info("$saksbehandlerId rekjører alle tasks med status $status")

        return Result.runCatching {
            taskRepository.finnTasksMedStatus(listOf(status), Pageable.unpaged())
                    .map { taskRepository.save(it.copy(triggerTid = LocalDateTime.now()).klarTilPlukk(saksbehandlerId)) }
        }
                .fold(
                        onSuccess = { Ressurs.success(data = "") },
                        onFailure = { e ->
                            logger.error("Rekjøring av tasker med status '$status' feilet", e)
                            Ressurs.failure(errorMessage = "Rekjøring av tasker med status '$status' feilet", error = e)
                        }
                )
    }

    @Transactional
    fun avvikshåndterTask(taskId: Long, avvikstype: Avvikstype, årsak: String, saksbehandlerId: String): Ressurs<String> {
        val task: Optional<Task> = taskRepository.findById(taskId)

        return when (task.isPresent) {
            false -> Ressurs.failure("Fant ikke task med id $taskId.")

            true -> {
                logger.info("$saksbehandlerId setter task $taskId til avvikshåndtert", taskId)

                Result.runCatching { taskRepository.save(task.get().avvikshåndter(avvikstype, årsak, saksbehandlerId)) }
                        .fold(
                                onSuccess = {
                                    Ressurs.success(data = "")
                                },
                                onFailure = { e ->
                                    logger.error("Avvikshåndtering av $taskId feilet", e)
                                    Ressurs.failure(errorMessage = "Avvikshåndtering av $taskId feilet", error = e)
                                }
                        )
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RestTaskService::class.java)
        const val TASK_LIMIT: Int = 100
    }
}
