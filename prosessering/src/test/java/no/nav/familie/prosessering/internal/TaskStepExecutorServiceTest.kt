package no.nav.familie.prosessering.internal

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction
import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class TaskStepExecutorServiceTest {

    @Autowired
    private lateinit var repository: TaskRepository

    @MockkBean(relaxUnitFun = true)
    lateinit var taskStep: TaskStep2

    @Autowired
    private lateinit var taskStepExecutorService: TaskStepExecutorService

    @Test
    fun `skal håndtere feil`() {
        var task2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='b'}")
        repository.save(task2)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThat(task2.status).isEqualTo(Status.UBEHANDLET)
        every { taskStep.doTask(any()) } throws (IllegalStateException())

        taskStepExecutorService.pollAndExecute()
        task2 = repository.findById(task2.id!!).orElseThrow()
        assertThat(task2.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(task2.logg).hasSize(3)

        taskStepExecutorService.pollAndExecute()
        task2 = repository.findById(task2.id!!).orElseThrow()
        assertThat(task2.status).isEqualTo(Status.KLAR_TIL_PLUKK)

        taskStepExecutorService.pollAndExecute()
        task2 = repository.findById(task2.id!!).orElseThrow()
        assertThat(task2.status).isEqualTo(Status.FEILET)
    }

    @Test
    fun `skal håndtere samtidighet`() {
        repeat(100) {
            val task2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='b'}")
            repository.save(task2)
        }
        TestTransaction.flagForCommit()
        TestTransaction.end()

        runBlocking {
            val launch = GlobalScope.launch {
                repeat(10) {taskStepExecutorService.pollAndExecute()}
            }
            val launch2 = GlobalScope.launch {
                repeat(10) {taskStepExecutorService.pollAndExecute()}
            }

            launch.join()
            launch2.join()
        }
        repository.findAll().filter { it.status != Status.FERDIG || it.logg.size > 4}.forEach{
            assertThat(it.status).isEqualTo(Status.FERDIG)
            assertThat(it.logg.size).isEqualTo(4)
        }

    }


}
