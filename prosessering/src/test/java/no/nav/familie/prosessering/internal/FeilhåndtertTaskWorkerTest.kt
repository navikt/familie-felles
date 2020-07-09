package no.nav.familie.prosessering.internal

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task.Companion.nyTask
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class FeilhåndtertTaskWorkerTest {

    @MockkBean(relaxUnitFun = true)
    lateinit var taskStep: TaskStep2

    @Autowired
    private lateinit var repository: TaskRepository

    @Autowired
    private lateinit var taskWorker: TaskWorker

    @Autowired
    private lateinit var worker: FeilhåndtertTaskWorker

    @Test
    fun `skal håndtere feil`() {
        var task2 = nyTask(TaskStep2.TASK_2, "{'a'='b'}")
        repository.saveAndFlush(task2)
        assertThat(task2.status).isEqualTo(Status.UBEHANDLET)
        every { taskStep.doTask(any()) } throws (IllegalStateException())

        worker.doActualWork(task2)
        task2 = repository.findById(task2.id!!).orElseThrow()
        assertThat(task2.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(task2.logg).hasSize(2)

        worker.doActualWork(task2)
        task2 = repository.findById(task2.id!!).orElseThrow()
        assertThat(task2.status).isEqualTo(Status.KLAR_TIL_PLUKK)

        worker.doActualWork(task2)
        task2 = repository.findById(task2.id!!).orElseThrow()
        assertThat(task2.status).isEqualTo(Status.FEILET)
    }
}
