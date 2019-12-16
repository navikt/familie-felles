package no.nav.familie.prosessering.rest

import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class RestTaskService(private val taskRepository: TaskRepository,
                      private val restTaskMapper: RestTaskMapper) {

    fun hentTasks(status: Status, saksbehandlerId: String): Ressurs<List<RestTask>> {
        logger.info("$saksbehandlerId henter feilede tasker")

        return Result.runCatching {
            taskRepository.finnTasksTilFrontend(status)
                    .map { restTaskMapper.toDto(it) }
        }
                .fold(
                        onSuccess = { Ressurs.success(data = it) },
                        onFailure = { e ->
                            logger.error("Henting av tasker feilet", e)
                            Ressurs.failure("Henting av tasker med status '$status', feilet.", e)
                        }
                )
    }

    @Transactional
    fun rekjørTask(taskId: Long, saksbehandlerId: String): Ressurs<String> {
        val task: Optional<Task> = taskRepository.findById(taskId)

        return when (task.isPresent) {
            true -> {
                taskRepository.save(task.get().klarTilPlukk(saksbehandlerId))
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
            taskRepository.finnTasksTilFrontend(status).map { taskRepository.save(it.klarTilPlukk(saksbehandlerId)) }
        }
                .fold(
                        onSuccess = { Ressurs.success(data = "") },
                        onFailure = { e ->
                            logger.error("Rekjøring av tasker med status '$status' feilet", e)
                            Ressurs.failure("Rekjøring av tasker med status '$status' feilet", e)
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
                                    Ressurs.failure("Avvikshåndtering av $taskId feilet", e)
                                }
                        )
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RestTaskService::class.java)
    }
}
