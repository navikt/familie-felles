package no.nav.familie.prosessering.rest

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
internal class TaskControllerIntegrasjonTest {

    @Autowired
    lateinit var restTaskService: RestTaskService
    @Autowired
    lateinit var repository: TaskRepository

    lateinit var taskController: TaskController

    @BeforeEach
    fun setup() {
        taskController = TaskController(restTaskService, mockk())
        every { taskController.hentBrukernavn() } returns ""

    }

    @Test
    fun `skal bare rekjøre tasker status FEILET`() {
        val ubehandletTask = Task.nyTask(TaskStep1.TASK_1, "{'a'='b'}")
        ubehandletTask.status = Status.UBEHANDLET
        val taskSomSkalRekjøres = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        taskSomSkalRekjøres.status = Status.FEILET
        val avvikshåndtert = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        avvikshåndtert.status = Status.AVVIKSHÅNDTERT
        repository.saveAndFlush(ubehandletTask)
        repository.saveAndFlush(taskSomSkalRekjøres)
        repository.saveAndFlush(avvikshåndtert)

        val response = taskController.rekjørTasks(Status.FEILET)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(repository.findById(taskSomSkalRekjøres.id!!).get().status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(repository.findById(ubehandletTask.id!!).get().status).isEqualTo(Status.UBEHANDLET)
        assertThat(repository.findById(avvikshåndtert.id!!).get().status).isEqualTo(Status.AVVIKSHÅNDTERT)
    }


}
